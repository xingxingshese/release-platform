<template>
  <div>
    <h2>发布计划</h2>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">加载中…</p>
    <table v-else>
      <thead>
        <tr><th>ID</th><th>名称</th><th>状态</th><th>环境</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="plan in plans" :key="plan.id">
          <td>{{ plan.id }}</td>
          <td>{{ plan.name }}</td>
          <td>{{ plan.status }}</td>
          <td>{{ plan.environments }}</td>
          <td class="actions">
            <button @click="act('ready', plan)" :disabled="plan.status !== 'DRAFT' || busy">就绪</button>
            <button @click="act('test-release', plan)" :disabled="plan.status !== 'READY' || busy">测试发布</button>
            <button @click="act('test-accept', plan)" :disabled="plan.status !== 'WAIT_TEST_ACCEPT' || busy">验收通过</button>
            <button @click="act('create-release-branch', plan)" :disabled="plan.status !== 'TEST_ACCEPTED' || busy">创建 Release Branch</button>
            <button @click="act('prod-confirm', plan)" :disabled="plan.status !== 'WAIT_PROD_CONFIRM' || busy">生产确认</button>
          </td>
        </tr>
      </tbody>
    </table>

    <h3>新建发布计划</h3>
    <form @submit.prevent="createPlan">
      <input v-model.number="form.projectId" type="number" placeholder="项目 ID" required />
      <input v-model="form.name" placeholder="计划名称" required />
      <input v-model="form.environments" placeholder="TEST,PRE,PROD 组合" />
      <button type="submit">创建</button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { api } from '../../api/client'
import type { ReleasePlan } from '../../types/release'

const plans = ref<ReleasePlan[]>([])
const loading = ref(true)
const error = ref('')
const busy = ref(false)
const form = reactive({ projectId: 1, name: '', environments: 'TEST' })

async function load() {
  error.value = ''
  try {
    plans.value = await api.get<ReleasePlan[]>('/api/release-plans')
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function act(action: string, plan: ReleasePlan) {
  busy.value = true
  error.value = ''
  try {
    await api.post(`/api/release-plans/${plan.id}/${action}`)
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    busy.value = false
  }
}

async function createPlan() {
  error.value = ''
  try {
    await api.post('/api/release-plans', form)
    form.name = ''
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
}

onMounted(load)
</script>

<style scoped>
.actions button { margin-right: 4px; }
.error { color: #c00; }
table { border-collapse: collapse; width: 100%; }
th, td { border: 1px solid #ddd; padding: 6px 10px; text-align: left; }
</style>
