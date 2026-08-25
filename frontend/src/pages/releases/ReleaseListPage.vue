<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">发布管理</h1>
      <el-button type="primary" @click="dialogVisible = true">新建发布计划</el-button>
    </div>

    <el-alert v-if="state.error.value" type="error" :title="state.error.value" show-icon @close="state.error.value = ''">
      <el-button link type="primary" @click="state.reload()">重试</el-button>
    </el-alert>

    <el-table v-loading="state.loading.value" :data="plans" border>
      <template #empty>
        <el-empty v-if="state.empty" description="暂无发布计划" />
      </template>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="versionName" label="版本" width="120" />
      <el-table-column label="状态" width="150">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="environments" label="环境" width="110" />
      <el-table-column label="操作" min-width="420">
        <template #default="{ row }">
          <el-button size="small" :disabled="!can(row, 'READY')" :loading="busy" @click="act(row, 'ready')">就绪</el-button>
          <el-button size="small" type="primary" :disabled="!can(row, 'READY')" :loading="busy" @click="act(row, 'test-release')">测试发布</el-button>
          <el-button size="small" :disabled="!can(row, 'WAIT_TEST_ACCEPT')" :loading="busy" @click="act(row, 'test-accept')">验收通过</el-button>
          <el-button size="small" :disabled="!can(row, 'TEST_ACCEPTED')" :loading="busy" @click="act(row, 'create-release-branch')">创建 RC</el-button>
          <el-button size="small" type="warning" :disabled="!can(row, 'RELEASE_BRANCH_CREATED', 'PRE_DEPLOY_SUCCESS')" :loading="busy" @click="act(row, 'deploy-pre')">预发</el-button>
          <el-button size="small" type="danger" :disabled="!can(row, 'PRE_DEPLOY_SUCCESS')" :loading="busy" @click="act(row, 'deploy-prod')">生产</el-button>
          <el-button size="small" type="success" :disabled="!can(row, 'WAIT_PROD_CONFIRM')" :loading="busy" @click="act(row, 'prod-confirm')">生产确认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建发布计划" width="480">
      <el-form label-width="90">
        <el-form-item label="项目 ID"><el-input-number v-model="form.projectId" :min="1" /></el-form-item>
        <el-form-item label="计划名称"><el-input v-model="form.name" placeholder="如：订单服务 8 月第 4 周" /></el-form-item>
        <el-form-item label="版本名"><el-input v-model="form.versionName" placeholder="如：v1.2.0（可空）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="createPlan">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { releaseApi } from '../../api/release'
import { useAsync } from '../../hooks/useAsync'
import { usePolling } from '../../hooks/usePolling'
import type { ReleasePlan } from '../../types/release'
import StatusTag from '../../components/status-tag/StatusTag.vue'

const state = useAsync<ReleasePlan[]>(() => releaseApi.list())
const plans = computed(() => state.data.value ?? [])
const busy = ref(false)
const dialogVisible = ref(false)
const form = reactive({ projectId: 1, name: '', versionName: '' })

// 状态轮询刷新（规范 §三十：先 Polling 后 SSE）
usePolling(() => state.reload())

type Action =
  | 'ready' | 'test-release' | 'test-accept'
  | 'create-release-branch' | 'deploy-pre' | 'deploy-prod' | 'prod-confirm'

function can(plan: ReleasePlan, ...statuses: string[]): boolean {
  return statuses.includes(plan.status)
}

async function act(plan: ReleasePlan, action: Action): Promise<void> {
  busy.value = true
  try {
    switch (action) {
      case 'ready':
        await releaseApi.ready(plan.id)
        break
      case 'test-release': {
        const task = await releaseApi.startTestRelease(plan.id)
        localStorage.setItem(`rap_last_task_${plan.id}`, String(task.id))
        ElMessage.success('测试发布已触发')
        break
      }
      case 'test-accept':
        await releaseApi.accept(plan.id)
        break
      case 'create-release-branch':
        await releaseApi.createReleaseBranch(plan.id)
        break
      case 'deploy-pre': {
        const task = await releaseApi.deployPre(plan.id)
        localStorage.setItem(`rap_last_task_${plan.id}`, String(task.id))
        ElMessage.success('预发发布已触发')
        break
      }
      case 'deploy-prod': {
        const task = await releaseApi.deployProd(plan.id)
        localStorage.setItem(`rap_last_task_${plan.id}`, String(task.id))
        ElMessage.success('生产发布已触发')
        break
      }
      case 'prod-confirm':
        await releaseApi.confirm(plan.id)
        ElMessage.success('生产确认完成，发布 COMPLETED')
        break
    }
    await state.reload()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  } finally {
    busy.value = false
  }
}

async function createPlan(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写计划名称')
    return
  }
  busy.value = true
  try {
    await releaseApi.create({ projectId: form.projectId, name: form.name.trim(), versionName: form.versionName || undefined })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    form.name = ''
    form.versionName = ''
    await state.reload()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
