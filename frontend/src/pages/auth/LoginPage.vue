<template>
  <div class="login-shell">
    <!-- 左侧品牌面板 -->
    <div class="brand-panel">
      <div class="grid-overlay" />
      <div class="brand-content">
        <div class="logo-row">
          <div class="logo-mark">R</div>
          <span class="logo-name">RAP Console</span>
        </div>
        <h1 class="headline">
          发布、验证与报警<br />
          <em>一条控制台</em>全部掌握
        </h1>
        <ul class="feature-list">
          <li><span class="k">✓</span> Jenkins SUCCESS ≠ 部署成功，四条件判定红线</li>
          <li><span class="k">✓</span> K8s 逐实例核验 · Health · Version 全通过</li>
          <li><span class="k">✓</span> 报警去重 · ACK · 自动升级 · 恢复通知</li>
          <li><span class="k">✓</span> 配置快照：发布不受后续配置修改影响</li>
        </ul>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="form-panel">
      <el-card class="login-card">
        <template #header>
          <div class="card-head">
            <span class="title">登录</span>
            <span class="hint">RAP · Release &amp; Alert Platform</span>
          </div>
        </template>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input v-model="username" placeholder="username" size="large" autofocus>
              <template #prefix><span class="mono">@</span></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="password" type="password" placeholder="••••••••" size="large" show-password />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" native-type="submit">
            {{ loading ? '登录中…' : '进入控制台' }}
          </el-button>
          <p v-if="error" class="error mono">{{ error }}</p>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../app/store/auth'

const auth = useAuthStore()
const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    void router.push('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  display: grid;
  grid-template-columns: minmax(420px, 1.2fr) 1fr;
  height: 100vh;
}

/* ---- 左侧品牌 ---- */
.brand-panel {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(800px 400px at 20% 0%, rgba(34, 211, 238, 0.12), transparent 60%),
    radial-gradient(600px 500px at 90% 100%, rgba(52, 211, 153, 0.08), transparent 60%),
    #0a0e13;
  border-right: 1px solid var(--rap-border-soft);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.025) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(700px 500px at 30% 30%, black, transparent);
}

.brand-content {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 8%;
}

.logo-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 48px;
}

.logo-mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 700;
  color: #06121a;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--rap-accent), #34d399);
  box-shadow: 0 0 28px rgba(34, 211, 238, 0.4);
}

.logo-name {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.06em;
}

.headline {
  font-size: clamp(28px, 3.4vw, 44px);
  font-weight: 700;
  line-height: 1.25;
  margin: 0 0 32px;
  letter-spacing: 0.01em;
}

.headline em {
  font-style: normal;
  color: var(--rap-accent);
  text-shadow: 0 0 24px rgba(34, 211, 238, 0.45);
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  color: var(--rap-text-dim);
  font-size: 14px;
}

.feature-list .k {
  font-family: var(--font-mono);
  color: var(--rap-ok);
  margin-right: 10px;
}

/* ---- 右侧表单 ---- */
.form-panel {
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.title {
  font-size: 18px;
  font-weight: 700;
}

.hint {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--rap-text-faint);
}

.mono {
  font-family: var(--font-mono);
}

.submit {
  width: 100%;
  margin-top: 6px;
}

.error {
  color: var(--rap-danger);
  font-size: 12px;
  margin: 12px 0 0;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
  .brand-panel {
    display: none;
  }
}
</style>
