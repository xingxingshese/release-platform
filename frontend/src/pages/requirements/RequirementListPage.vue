<template>
  <div>
    <h1 class="page-title">需求管理</h1>
    <el-alert v-if="error" type="error" :title="error" show-icon>
      <el-button link type="primary" @click="load">重试</el-button>
    </el-alert>

    <el-table v-loading="loading" :data="requirements" border>
      <template #empty><el-empty description="暂无需求（选择项目后加载）" /></template>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="source" label="来源" width="110" />
      <el-table-column prop="externalKey" label="外部 Key" width="160" />
      <el-table-column prop="status" label="状态" width="110" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { projectApi } from '../../api/project'
import type { Requirement } from '../../api/project'

const projectId = ref<number>(1)
const requirements = ref<Requirement[]>([])
const loading = ref(false)
const error = ref('')

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    requirements.value = await projectApi.requirements(projectId.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

watch(projectId, load, { immediate: true })
</script>
