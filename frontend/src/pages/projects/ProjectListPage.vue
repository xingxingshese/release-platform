<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">项目管理</h1>
      <el-button type="primary" @click="dialogVisible = true">新建项目</el-button>
    </div>

    <el-alert v-if="state.error.value" type="error" :title="state.error.value" show-icon>
      <el-button link type="primary" @click="state.reload()">重试</el-button>
    </el-alert>

    <el-table v-loading="state.loading.value" :data="projects" border>
      <template #empty><el-empty v-if="state.empty" description="暂无项目" /></template>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="项目名称" min-width="200" />
      <el-table-column prop="code" label="编码" width="140" />
      <el-table-column prop="projectType" label="类型" width="120" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建项目" width="460">
      <el-form label-width="90">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" placeholder="如 order-service" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.projectType">
            <el-option label="后端" value="BACKEND" />
            <el-option label="前端" value="FRONTEND" />
            <el-option label="全栈" value="FULLSTACK" />
            <el-option label="混合" value="MIXED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi } from '../../api/project'
import { useAsync } from '../../hooks/useAsync'
import type { Project } from '../../types/project'

const state = useAsync<Project[]>(() => projectApi.list())
const projects = computed(() => state.data.value ?? [])
const busy = ref(false)
const dialogVisible = ref(false)
const form = reactive({ name: '', code: '', projectType: 'BACKEND' as Project['projectType'] })

async function create(): Promise<void> {
  if (!form.name.trim() || !form.code.trim()) {
    ElMessage.warning('请填写名称与编码')
    return
  }
  busy.value = true
  try {
    await projectApi.create({ name: form.name.trim(), code: form.code.trim(), projectType: form.projectType })
    ElMessage.success('创建成功')
    dialogVisible.value = false
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
