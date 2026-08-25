<template>
  <div>
    <div class="page-header"><h1 class="page-title">管理员配置中心</h1></div>

    <el-card class="section">
      <template #header>选择配置（类型 / Key）</template>
      <el-form inline>
        <el-form-item label="配置类型">
          <el-select v-model="configType" style="width: 180px">
            <el-option v-for="t in TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="Key">
          <el-input v-model="configKey" placeholder="如 prod" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="history.loading.value" @click="loadHistory">查看版本</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert v-if="history.error.value" type="error" :title="history.error.value" show-icon />

    <el-card class="section">
      <template #header>版本历史（新→旧）</template>
      <el-table v-loading="history.loading.value" :data="versions" border size="small">
        <template #empty><el-empty description="无版本记录" /></template>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="changedBy" label="修改人 ID" width="110" />
        <el-table-column prop="reason" label="变更原因" min-width="220" />
      </el-table>
      <div class="diff-actions">
        <span>对比：</span>
        <el-input-number v-model="v1" :min="1" size="small" />
        <span>vs</span>
        <el-input-number v-model="v2" :min="1" size="small" />
        <el-button size="small" type="primary" @click="loadDiff">对比当前值 vs 新值</el-button>
        <el-button size="small" @click="saveDialogVisible = true">保存新版本…</el-button>
      </div>
    </el-card>

    <el-card class="section" v-if="diffs.length > 0">
      <template #header>字段级 Diff（规范 §三十三：当前值 / 新值）</template>
      <el-table :data="diffs" border size="small">
        <el-table-column prop="path" label="字段路径" min-width="180" />
        <el-table-column prop="before" label="旧值" min-width="150" />
        <el-table-column prop="after" label="新值" min-width="150" />
      </el-table>
    </el-card>

    <el-dialog v-model="saveDialogVisible" title="保存新版本（产生新 config_version）" width="520">
      <el-form label-width="90">
        <el-form-item label="JSON 内容">
          <el-input v-model="saveContent" type="textarea" :rows="6" placeholder='{"replicas":2}' />
        </el-form-item>
        <el-form-item label="变更原因"><el-input v-model="saveReason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVersion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/admin'
import { useAsync } from '../../hooks/useAsync'
import type { ConfigDiffItem, ConfigVersionSummary } from '../../types/project'

const TYPES = ['environment', 'jenkins', 'deployment', 'notification', 'alert', 'system']

const configType = ref('environment')
const configKey = ref('prod')

const history = useAsync<ConfigVersionSummary[]>(
  () => adminApi.configVersions(configType.value, configKey.value),
  false
)
const versions = computed(() => history.data.value ?? [])

async function loadHistory(): Promise<void> {
  await history.reload()
}

const v1 = ref(1)
const v2 = ref(2)
const diffs = ref<ConfigDiffItem[]>([])

async function loadDiff(): Promise<void> {
  try {
    diffs.value = await adminApi.configDiff(configType.value, configKey.value, v1.value, v2.value)
    if (diffs.value.length === 0) {
      ElMessage.info('两个版本内容一致')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
}

const saveDialogVisible = ref(false)
const saveContent = ref('')
const saveReason = ref('')
const saving = ref(false)

async function saveVersion(): Promise<void> {
  saving.value = true
  try {
    const result = await adminApi.saveConfig(configType.value, configKey.value, saveContent.value, saveReason.value)
    ElMessage.success(`已保存为 v${result.version}，已执行的发布不受影响`)
    saveDialogVisible.value = false
    await loadHistory()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.section {
  margin-bottom: 16px;
}
.diff-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
