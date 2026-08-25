<template>
  <el-steps :active="activeIndex" align-center finish-status="success">
    <el-step
      v-for="step in steps"
      :key="step.key"
      :title="step.label"
      :status="stepStatus(step)"
    />
  </el-steps>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ReleaseStatus } from '../../types/release'
import type { ReleaseTimelineStep } from '../../types/deployment'

/**
 * 发布详情 Timeline（规范 §三十一）：
 * 需求→代码→Merge→Jenkins→测试环境→验收→Release Branch→预发→生产→确认。
 */
const props = defineProps<{ status: ReleaseStatus }>()

const CHAIN: Array<{ key: string; label: string; reached: ReleaseStatus[] }> = [
  { key: 'plan', label: '发布计划', reached: ['DRAFT', 'READY', 'TEST_MERGING', 'WAIT_CONFLICT_RESOLVE', 'TEST_DEPLOYING', 'TEST_DEPLOY_SUCCESS', 'WAIT_TEST_ACCEPT', 'TEST_REJECTED', 'TEST_ACCEPTED', 'RELEASE_BRANCH_CREATING', 'RELEASE_BRANCH_CREATED', 'PRE_DEPLOYING', 'PRE_DEPLOY_SUCCESS', 'PROD_DEPLOYING', 'PROD_DEPLOY_SUCCESS', 'WAIT_PROD_CONFIRM', 'COMPLETED'] },
  { key: 'test-release', label: '测试环境', reached: ['TEST_DEPLOYING', 'TEST_DEPLOY_SUCCESS', 'WAIT_TEST_ACCEPT', 'TEST_ACCEPTED', 'RELEASE_BRANCH_CREATING', 'RELEASE_BRANCH_CREATED', 'PRE_DEPLOYING', 'PRE_DEPLOY_SUCCESS', 'PROD_DEPLOYING', 'PROD_DEPLOY_SUCCESS', 'WAIT_PROD_CONFIRM', 'COMPLETED'] },
  { key: 'acceptance', label: '测试验收', reached: ['TEST_ACCEPTED', 'RELEASE_BRANCH_CREATING', 'RELEASE_BRANCH_CREATED', 'PRE_DEPLOYING', 'PRE_DEPLOY_SUCCESS', 'PROD_DEPLOYING', 'PROD_DEPLOY_SUCCESS', 'WAIT_PROD_CONFIRM', 'COMPLETED'] },
  { key: 'release-branch', label: 'Release Branch', reached: ['RELEASE_BRANCH_CREATED', 'PRE_DEPLOYING', 'PRE_DEPLOY_SUCCESS', 'PROD_DEPLOYING', 'PROD_DEPLOY_SUCCESS', 'WAIT_PROD_CONFIRM', 'COMPLETED'] },
  { key: 'pre', label: '预发', reached: ['PRE_DEPLOY_SUCCESS', 'PROD_DEPLOYING', 'PROD_DEPLOY_SUCCESS', 'WAIT_PROD_CONFIRM', 'COMPLETED'] },
  { key: 'prod', label: '生产', reached: ['PROD_DEPLOY_SUCCESS', 'WAIT_PROD_CONFIRM', 'COMPLETED'] },
  { key: 'confirm', label: '生产确认', reached: ['COMPLETED'] }
]

const steps = computed<ReleaseTimelineStep[]>(() =>
  CHAIN.map((c) => ({
    key: c.key,
    label: c.label,
    status: stepState(c)
  }))
)

function stepState(c: (typeof CHAIN)[number]): ReleaseTimelineStep['status'] {
  if (c.reached.includes(props.status)) return 'done'
  if (isFailure()) return 'failed'
  if (isActive(c)) return 'active'
  return 'pending'
}

function isActive(c: (typeof CHAIN)[number]): boolean {
  const transitions: Partial<Record<ReleaseStatus, string>> = {
    TEST_MERGING: 'test-release',
    WAIT_CONFLICT_RESOLVE: 'test-release',
    TEST_DEPLOYING: 'test-release',
    WAIT_TEST_ACCEPT: 'acceptance',
    RELEASE_BRANCH_CREATING: 'release-branch',
    PRE_DEPLOYING: 'pre',
    PROD_DEPLOYING: 'prod',
    WAIT_PROD_CONFIRM: 'confirm'
  }
  return transitions[props.status] === c.key
}

function isFailure(): boolean {
  return ['FAILED', 'TIMEOUT', 'CANCELLED'].includes(props.status)
}

const activeIndex = computed(() => {
  const idx = CHAIN.findIndex((c) => c.reached.includes(props.status))
  return idx < 0 ? 0 : idx
})

function stepStatus(step: ReleaseTimelineStep): 'success' | 'error' | 'process' | 'wait' {
  if (step.status === 'done') return 'success'
  if (step.status === 'failed') return 'error'
  if (step.status === 'active') return 'process'
  return 'wait'
}
</script>
