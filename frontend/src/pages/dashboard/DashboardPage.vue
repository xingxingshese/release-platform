<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Dashboard</h1>
      <span class="refresh-hint">每 5s 自动刷新</span>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">发布计划总数</div>
          <div class="metric-num stat-value">{{ planCount }}</div>
          <div class="stat-spark accent" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">进行中发布</div>
          <div class="metric-num stat-value warn">{{ activeCount }}</div>
          <div class="stat-spark amber" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">未恢复报警</div>
          <div class="metric-num stat-value danger">{{ openAlertCount }}</div>
          <div class="stat-spark rose" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts">
      <el-col :span="12">
        <el-card>
          <template #header>发布状态分布</template>
          <div ref="statusChartEl" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>报警级别分布</template>
          <div ref="alertChartEl" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watchEffect } from 'vue'
import * as echarts from 'echarts'
import { releaseApi } from '../../api/release'
import { alertApi } from '../../api/alert'
import { useAsync } from '../../hooks/useAsync'
import type { ReleasePlan } from '../../types/release'
import type { Alert } from '../../types/alert'

const state = useAsync<ReleasePlan[]>(() => releaseApi.list())
const alertState = useAsync<Alert[]>(() => alertApi.list())

const plans = computed(() => state.data.value ?? [])
const alerts = computed(() => alertState.data.value ?? [])

const RUNNING_STATES = new Set([
  'TEST_MERGING', 'WAIT_CONFLICT_RESOLVE', 'TEST_DEPLOYING', 'WAIT_TEST_ACCEPT',
  'RELEASE_BRANCH_CREATING', 'PRE_DEPLOYING', 'PROD_DEPLOYING', 'WAIT_PROD_CONFIRM'
])
const OPEN_ALERTS = new Set(['ALERTING', 'ACKNOWLEDGED'])

const planCount = computed(() => plans.value.length)
const activeCount = computed(() => plans.value.filter((p) => RUNNING_STATES.has(p.status)).length)
const openAlertCount = computed(() => alerts.value.filter((a) => OPEN_ALERTS.has(a.status)).length)

const statusChartEl = ref<HTMLElement | null>(null)
const alertChartEl = ref<HTMLElement | null>(null)
let statusChart: echarts.ECharts | null = null
let alertChart: echarts.ECharts | null = null

const AXIS_STYLE = {
  axisLine: { lineStyle: { color: '#2a3542' } },
  axisLabel: { color: '#8a97a8', fontSize: 10 }
}

function renderStatusChart(): void {
  if (!statusChartEl.value) return
  const counts = new Map<string, number>()
  for (const p of plans.value) {
    counts.set(p.status, (counts.get(p.status) ?? 0) + 1)
  }
  statusChart = statusChart ?? echarts.init(statusChartEl.value)
  statusChart.setOption({
    tooltip: { backgroundColor: '#171e28', borderColor: '#1f2733', textStyle: { color: '#dbe4ee' } },
    grid: { left: 40, right: 16, top: 16, bottom: 56 },
    xAxis: {
      type: 'category',
      data: [...counts.keys()],
      axisLabel: { ...AXIS_STYLE.axisLabel, rotate: 40 },
      axisLine: AXIS_STYLE.axisLine
    },
    yAxis: { type: 'value', minInterval: 1, ...AXIS_STYLE },
    series: [
      {
        type: 'bar',
        barMaxWidth: 26,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#22d3ee' },
            { offset: 1, color: 'rgba(34,211,238,0.15)' }
          ])
        },
        data: [...counts.values()]
      }
    ]
  })
}

function renderAlertChart(): void {
  if (!alertChartEl.value) return
  const levels = ['INFO', 'WARN', 'CRITICAL']
  const colors = ['#64748b', '#fbbf24', '#fb7185']
  const data = levels.map((l) => alerts.value.filter((a) => a.level === l).length)
  alertChart = alertChart ?? echarts.init(alertChartEl.value)
  alertChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#171e28', borderColor: '#1f2733', textStyle: { color: '#dbe4ee' } },
    legend: { bottom: 0, textStyle: { color: '#8a97a8' } },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        itemStyle: { borderColor: '#141a22', borderWidth: 2 },
        label: { color: '#8a97a8', fontFamily: 'JetBrains Mono', fontSize: 11 },
        data: levels.map((l, i) => ({ name: l, value: data[i], itemStyle: { color: colors[i] } }))
      }
    ]
  })
}

watchEffect(() => {
  if (!state.loading.value && !alertState.loading.value) {
    renderStatusChart()
    renderAlertChart()
  }
})

onMounted(() => window.addEventListener('resize', resize))
onUnmounted(() => {
  window.removeEventListener('resize', resize)
  statusChart?.dispose()
  alertChart?.dispose()
})

function resize(): void {
  statusChart?.resize()
  alertChart?.resize()
}
</script>

<style scoped>
.refresh-hint {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--rap-text-faint);
}

.stat-card {
  position: relative;
  overflow: hidden;
  background: var(--rap-panel);
  border: 1px solid var(--rap-border-soft);
  border-radius: 12px;
  padding: 18px 20px;
  transition: transform 0.15s ease, border-color 0.15s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(34, 211, 238, 0.35);
}

.stat-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--rap-text-faint);
}

.stat-value {
  font-size: 40px;
  font-weight: 700;
  line-height: 1.2;
  margin-top: 6px;
}

.stat-value.warn { color: var(--rap-warn); }
.stat-value.danger { color: var(--rap-danger); }

.stat-spark {
  position: absolute;
  inset: auto 0 0 0;
  height: 3px;
}
.stat-spark.accent { background: linear-gradient(90deg, transparent, var(--rap-accent)); }
.stat-spark.amber { background: linear-gradient(90deg, transparent, var(--rap-warn)); }
.stat-spark.rose { background: linear-gradient(90deg, transparent, var(--rap-danger)); }

.charts {
  margin-top: 16px;
}

.chart {
  height: 300px;
}
</style>
