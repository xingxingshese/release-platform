<template>
  <div>
    <div class="page-header"><h1 class="page-title">报警中心</h1></div>
    <el-alert v-if="state.error.value" type="error" :title="state.error.value" show-icon>
      <el-button link type="primary" @click="state.reload()">重试</el-button>
    </el-alert>

    <el-table v-loading="state.loading.value" :data="alerts" border>
      <template #empty><el-empty v-if="state.empty" description="当前无报警 🎉" /></template>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="row.level === 'CRITICAL' ? 'danger' : row.level === 'WARN' ? 'warning' : 'info'" size="small">
            {{ row.level }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="count" label="次数" width="80" />
      <el-table-column prop="escalationLevel" label="升级级别" width="90" />
      <el-table-column prop="environment" label="环境" width="90" />
      <el-table-column prop="service" label="服务" width="130" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" type="warning" :disabled="row.status !== 'ALERTING'" @click="ack(row)">ACK</el-button>
          <el-button size="small" type="success" :disabled="row.status === 'RESOLVED'" @click="resolve(row)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { alertApi } from '../../api/alert'
import { useAsync } from '../../hooks/useAsync'
import { usePolling } from '../../hooks/usePolling'
import type { Alert } from '../../types/alert'
import StatusTag from '../../components/status-tag/StatusTag.vue'

const state = useAsync<Alert[]>(() => alertApi.list())
const alerts = computed(() => state.data.value ?? [])

// 报警实时性：5s 轮询
usePolling(() => state.reload())

async function ack(alert: Alert): Promise<void> {
  try {
    await alertApi.ack(alert.id)
    ElMessage.success('已 ACK：停止普通重复通知（超时未恢复仍会升级）')
    await state.reload()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
}

async function resolve(alert: Alert): Promise<void> {
  try {
    await alertApi.resolve(alert.id)
    ElMessage.success('已标记恢复')
    await state.reload()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
}
</script>
