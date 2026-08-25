<template>
  <div v-loading="state.loading.value">
    <el-page-header @back="goBack" class="detail-header">
      <template #content>
        <span class="title">{{ plan?.name ?? `发布计划 #${planId}` }}</span>
        <StatusTag v-if="plan" :status="plan.status" />
      </template>
    </el-page-header>

    <el-alert v-if="state.error.value" type="error" :title="state.error.value" show-icon>
      <el-button link type="primary" @click="state.reload()">重试</el-button>
    </el-alert>

    <el-card class="section">
      <ReleaseTimeline v-if="plan" :status="plan.status" />
    </el-card>

    <el-card class="section">
      <template #header>部署节点明细（Deployment / Pod / Health / Version）</template>
      <DeploymentNodeTable :nodes="nodes" :loading="nodesLoading" />
      <el-empty v-if="!nodesLoading && nodes.length === 0" description="暂无部署记录：发起发布后此处展示逐实例判定" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { releaseApi } from '../../api/release'
import { useAsync } from '../../hooks/useAsync'
import { usePolling } from '../../hooks/usePolling'
import type { DeploymentNode } from '../../types/deployment'
import type { ReleasePlan } from '../../types/release'
import ReleaseTimeline from '../../components/release-timeline/ReleaseTimeline.vue'
import DeploymentNodeTable from '../../components/deployment-status/DeploymentNodeTable.vue'
import StatusTag from '../../components/status-tag/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const planId = computed(() => Number(route.params.id))

const state = useAsync<ReleasePlan>(() => releaseApi.detail(planId.value))
const plan = computed(() => state.data.value)

const nodes = ref<DeploymentNode[]>([])
const nodesLoading = ref(false)

async function refreshNodes(): Promise<void> {
  nodesLoading.value = true
  try {
    // 后端契约：GET /api/release-tasks/{taskId}/nodes；taskId 在列表页触发发布时记录
    const taskId = Number(localStorage.getItem(`rap_last_task_${planId.value}`) ?? 0)
    if (taskId > 0) {
      nodes.value = await releaseApi.deploymentNodes(taskId)
    }
  } catch {
    nodes.value = []
  } finally {
    nodesLoading.value = false
  }
}

usePolling(async () => {
  await state.reload()
  await refreshNodes()
})

watch(
  () => planId.value,
  () => void state.reload()
)

function goBack(): void {
  void router.push({ name: 'releases' })
}
</script>

<style scoped>
.detail-header {
  margin-bottom: 16px;
}
.title {
  font-weight: 600;
  margin-right: 12px;
}
.section {
  margin-bottom: 16px;
}
</style>
