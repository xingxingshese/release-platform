<template>
  <div>
    <h2>报警中心</h2>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">加载中…</p>
    <table v-else>
      <thead>
        <tr><th>ID</th><th>标题</th><th>级别</th><th>状态</th><th>环境</th><th>服务</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="alert in alerts" :key="alert.id">
          <td>{{ alert.id }}</td>
          <td>{{ alert.title }}</td>
          <td>{{ alert.level }}</td>
          <td>{{ alert.status }}</td>
          <td>{{ alert.environment }}</td>
          <td>{{ alert.service }}</td>
          <td class="actions">
            <button @click="ack(alert)" :disabled="alert.status !== 'ALERTING' || busy">处理中(ACK)</button>
            <button @click="resolve(alert)" :disabled="alert.status === 'RESOLVED' || busy">恢复</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../../api/client'

interface Alert {
  id: number
  title: string
  level: string
  status: string
  environment: string | null
  service: string | null
}

const alerts = ref<Alert[]>([])
const loading = ref(true)
const error = ref('')
const busy = ref(false)

async function load() {
  try {
    alerts.value = await api.get<Alert[]>('/api/alerts')
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function ack(alert: Alert) {
  busy.value = true
  try { await api.post(`/api/alerts/${alert.id}/ack`); await load() }
  catch (e) { error.value = e instanceof Error ? e.message : String(e) }
  finally { busy.value = false }
}

async function resolve(alert: Alert) {
  busy.value = true
  try { await api.post(`/api/alerts/${alert.id}/resolve`); await load() }
  catch (e) { error.value = e instanceof Error ? e.message : String(e) }
  finally { busy.value = false }
}

onMounted(load)
</script>

<style scoped>
.actions button { margin-right: 4px; }
.error { color: #c00; }
table { border-collapse: collapse; width: 100%; }
th, td { border: 1px solid #ddd; padding: 6px 10px; text-align: left; }
</style>
