<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">R</div>
        <div>
          <div class="brand-name">RAP Console</div>
          <div class="brand-sub">发布与统一报警平台</div>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router>
        <div class="menu-group">概览</div>
        <el-menu-item index="/">
          <span>Dashboard</span>
        </el-menu-item>

        <div class="menu-group">交付</div>
        <el-menu-item index="/releases">
          <span>发布管理</span>
        </el-menu-item>
        <el-menu-item index="/projects">
          <span>项目管理</span>
        </el-menu-item>
        <el-menu-item index="/requirements">
          <span>需求管理</span>
        </el-menu-item>

        <div class="menu-group">运维</div>
        <el-menu-item index="/alerts">
          <span>报警中心</span>
          <span v-if="alertCount > 0" class="badge">{{ alertCount }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/configs">
          <span>管理员配置</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <span class="pulse-dot" /> 系统运行中
      </div>
    </el-aside>

    <el-container class="main-col">
      <el-header class="header">
        <div class="env-chip">
          <span class="chip-dot" /> DEV
        </div>
        <div class="spacer" />
        <span v-if="auth.username" class="user">
          <span class="avatar">{{ auth.username.slice(0, 1).toUpperCase() }}</span>
          {{ auth.username }}
        </span>
        <el-button link type="danger" @click="logout">退出</el-button>
      </el-header>
      <el-main class="content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RouterView } from 'vue-router'
import { useAuthStore } from '../app/store/auth'
import { alertApi } from '../api/alert'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

// 报警角标：30s 轻量轮询
const alertCount = ref(0)
let timer: ReturnType<typeof setInterval> | null = null
onMounted(async () => {
  const load = async () => {
    try {
      const list = await alertApi.list()
      alertCount.value = list.filter((a) => a.status !== 'RESOLVED').length
    } catch {
      /* 静默 */
    }
  }
  await load()
  timer = setInterval(load, 30000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function logout(): void {
  auth.logout()
  void router.push({ name: 'login' })
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background: rgba(10, 13, 18, 0.85);
  border-right: 1px solid var(--rap-border-soft);
  backdrop-filter: blur(6px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px 16px;
  border-bottom: 1px solid var(--rap-border-soft);
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 20px;
  color: #06121a;
  background: linear-gradient(135deg, var(--rap-accent), #34d399);
  border-radius: 10px;
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.35);
}

.brand-name {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.04em;
}

.brand-sub {
  font-size: 11px;
  color: var(--rap-text-faint);
  letter-spacing: 0.08em;
}

.menu-group {
  padding: 14px 22px 6px;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--rap-text-faint);
}

.badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  font-family: var(--font-mono);
  font-size: 11px;
  border-radius: 9px;
  background: rgba(251, 113, 133, 0.15);
  color: var(--rap-danger);
  box-shadow: 0 0 8px rgba(251, 113, 133, 0.35);
}

.sidebar-footer {
  margin-top: auto;
  padding: 14px 20px;
  font-size: 11px;
  color: var(--rap-text-faint);
  border-top: 1px solid var(--rap-border-soft);
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono);
}

.pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--rap-ok);
  box-shadow: 0 0 8px var(--rap-ok);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  50% {
    opacity: 0.4;
  }
}

.header {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 52px;
  border-bottom: 1px solid var(--rap-border-soft);
  background: rgba(10, 13, 18, 0.6);
}

.env-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--rap-warn);
  border: 1px solid rgba(251, 191, 36, 0.3);
  background: rgba(251, 191, 36, 0.08);
  padding: 3px 10px;
  border-radius: 999px;
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--rap-warn);
  box-shadow: 0 0 6px var(--rap-warn);
}

.spacer {
  flex: 1;
}

.user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--rap-text-dim);
  font-size: 13px;
}

.avatar {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-weight: 700;
  font-size: 12px;
  color: var(--rap-accent);
  background: rgba(34, 211, 238, 0.12);
  border: 1px solid rgba(34, 211, 238, 0.35);
}
</style>
