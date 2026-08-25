<template>
  <span class="status-pill" :class="tone">
    <span class="dot" :class="{ blink: isBlinking }" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const LABELS: Record<string, string> = {
  DRAFT: '草稿', READY: '就绪', TEST_MERGING: '合并中',
  WAIT_CONFLICT_RESOLVE: '冲突待解决', TEST_DEPLOYING: '测试部署中',
  TEST_DEPLOY_SUCCESS: '测试部署成功', WAIT_TEST_ACCEPT: '待验收',
  TEST_REJECTED: '验收驳回', TEST_ACCEPTED: '已验收',
  RELEASE_BRANCH_CREATING: '创建 RC 中', RELEASE_BRANCH_CREATED: 'RC 已创建',
  PRE_DEPLOYING: '预发部署中', PRE_DEPLOY_SUCCESS: '预发成功',
  PROD_DEPLOYING: '生产部署中', PROD_DEPLOY_SUCCESS: '生产部署成功',
  WAIT_PROD_CONFIRM: '待生产确认', COMPLETED: '已完成',
  FAILED: '失败', TIMEOUT: '超时', CANCELLED: '已取消',
  ALERTING: '报警中', ACKNOWLEDGED: '已确认', RESOLVED: '已恢复',
  SUCCESS: '成功', RUNNING: '进行中'
}

const FAILURE = new Set(['FAILED', 'TIMEOUT', 'CANCELLED', 'TEST_REJECTED'])
const ATTENTION = new Set(['WAIT_CONFLICT_RESOLVE', 'ALERTING'])
const RUNNING = new Set([
  'TEST_MERGING', 'TEST_DEPLOYING', 'RELEASE_BRANCH_CREATING', 'PRE_DEPLOYING',
  'PROD_DEPLOYING', 'RUNNING'
])
const SUCCESS = new Set(['COMPLETED', 'RESOLVED', 'SUCCESS'])

const label = computed(() => LABELS[props.status] ?? props.status)
const tone = computed(() => {
  if (FAILURE.has(props.status)) return 'danger'
  if (ATTENTION.has(props.status)) return 'alarm'
  if (RUNNING.has(props.status)) return 'running'
  if (SUCCESS.has(props.status)) return 'ok'
  if (props.status.startsWith('WAIT_') || props.status === 'ACKNOWLEDGED') return 'wait'
  return 'idle'
})
const isBlinking = computed(() => tone.value === 'running' || tone.value === 'alarm')
</script>

<style scoped>
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  font-family: var(--font-mono);
  letter-spacing: 0.02em;
  white-space: nowrap;
  border: 1px solid transparent;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex: none;
}

.blink {
  animation: blink 1.2s ease-in-out infinite;
}

@keyframes blink {
  50% {
    opacity: 0.25;
  }
}

.ok {
  color: var(--rap-ok);
  background: rgba(52, 211, 153, 0.08);
  border-color: rgba(52, 211, 153, 0.25);
}
.ok .dot { background: var(--rap-ok); box-shadow: 0 0 6px var(--rap-ok); }

.danger {
  color: var(--rap-danger);
  background: rgba(251, 113, 133, 0.08);
  border-color: rgba(251, 113, 133, 0.3);
}
.danger .dot { background: var(--rap-danger); box-shadow: 0 0 6px var(--rap-danger); }

.alarm {
  color: #fda4af;
  background: rgba(251, 113, 133, 0.12);
  border-color: rgba(251, 113, 133, 0.4);
}
.alarm .dot { background: var(--rap-danger); box-shadow: 0 0 8px var(--rap-danger); }

.running {
  color: var(--rap-accent);
  background: rgba(34, 211, 238, 0.07);
  border-color: rgba(34, 211, 238, 0.3);
}
.running .dot { background: var(--rap-accent); box-shadow: 0 0 6px var(--rap-accent); }

.wait {
  color: var(--rap-warn);
  background: rgba(251, 191, 36, 0.07);
  border-color: rgba(251, 191, 36, 0.28);
}
.wait .dot { background: var(--rap-warn); box-shadow: 0 0 6px var(--rap-warn); }

.idle {
  color: var(--rap-text-dim);
  background: rgba(255, 255, 255, 0.03);
  border-color: var(--rap-border);
}
.idle .dot { background: var(--rap-text-faint); }
</style>
