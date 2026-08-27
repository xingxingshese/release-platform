<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { ConfigDiffItem, ConfigVersionItem } from "@/api/types";
import {
  diffConfigVersions,
  listConfigVersions,
  saveConfigVersion
} from "@/api/admin";

defineOptions({ name: "AdminConfig" });

/** 配置类型分组（对应 specs/016-admin-config 的配置化资源） */
const CONFIG_TYPES = [
  "PROJECT",
  "ENVIRONMENT",
  "RELEASE_FLOW",
  "GIT",
  "JENKINS",
  "DEPLOYMENT",
  "HEALTH_CHECK",
  "VERSION_CHECK",
  "NOTIFICATION",
  "ALERT",
  "RBAC",
  "SYSTEM"
] as const;

const form = reactive({
  type: "JENKINS" as (typeof CONFIG_TYPES)[number],
  key: ""
});

const loading = ref(false);
const versions = ref<ConfigVersionItem[]>([]);

async function loadVersions() {
  if (!form.key) return;
  loading.value = true;
  try {
    versions.value = (await listConfigVersions(form.type, form.key)) ?? [];
  } catch {
    versions.value = [];
  } finally {
    loading.value = false;
  }
}

// ---- 保存新版本 ----
const editorVisible = ref(false);
const editorForm = reactive({ content: "{}", reason: "" });
const saving = ref(false);

function openEditor() {
  Object.assign(editorForm, { content: "{\n  \n}", reason: "" });
  editorVisible.value = true;
}

async function submitSave() {
  if (!form.key) {
    ElMessage.warning("请先填写配置 Key");
    return;
  }
  saving.value = true;
  try {
    const saved = await saveConfigVersion(form.type, form.key, {
      content: editorForm.content,
      reason: editorForm.reason || undefined
    });
    ElMessage.success(`已保存为 V${saved.version}`);
    editorVisible.value = false;
    loadVersions();
  } finally {
    saving.value = false;
  }
}

// ---- 版本对比 ----
const diffVisible = ref(false);
const diffLoading = ref(false);
const diffItems = ref<ConfigDiffItem[]>([]);
const diffPair = reactive({ v1: 0, v2: 0 });

function openDiff() {
  if (versions.value.length < 1) {
    ElMessage.warning("至少需要两个版本才能对比");
    return;
  }
  const latest = versions.value[0]?.version ?? 0;
  const prev = versions.value.find(v => v.version < latest)?.version ?? latest;
  diffPair.v1 = prev;
  diffPair.v2 = latest;
  runDiff();
}

async function runDiff() {
  diffLoading.value = true;
  diffVisible.value = true;
  try {
    diffItems.value =
      (await diffConfigVersions(
        form.type,
        form.key,
        diffPair.v1,
        diffPair.v2
      )) ?? [];
  } catch {
    diffItems.value = [];
  } finally {
    diffLoading.value = false;
  }
}

const canDiff = computed(() => form.key && versions.value.length >= 2);
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex justify-between items-center flex-wrap gap-2">
        <span>配置中心（配置版本 + 字段级 Diff）</span>
        <div class="flex items-center gap-2">
          <el-select v-model="form.type" class="!w-44" @change="versions = []">
            <el-option
              v-for="t in CONFIG_TYPES"
              :key="t"
              :label="t"
              :value="t"
            />
          </el-select>
          <el-input
            v-model="form.key"
            placeholder="配置 Key，如 jenkins-server-01"
            class="!w-56"
            @keyup.enter="loadVersions"
          />
          <el-button type="primary" :disabled="!form.key" @click="loadVersions">
            查询版本历史
          </el-button>
        </div>
      </div>
    </template>

    <div class="flex gap-2 mb-4">
      <el-button type="primary" :disabled="!form.key" @click="openEditor">
        保存新版本
      </el-button>
      <el-button :disabled="!canDiff" @click="openDiff">版本对比</el-button>
    </div>

    <el-table v-loading="loading" :data="versions" stripe border>
      <el-table-column prop="version" label="版本" width="100">
        <template #default="{ row }">
          <el-tag size="small">V{{ row.version }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="changedBy" label="修改人（用户ID）" width="160" />
      <el-table-column prop="reason" label="变更原因" min-width="240" />
    </el-table>

    <!-- 新建版本 -->
    <el-dialog v-model="editorVisible" title="保存配置新版本" width="640px">
      <el-form label-width="90px">
        <el-form-item label="配置项">
          <span>{{ form.type }} / {{ form.key || "-" }}</span>
        </el-form-item>
        <el-form-item label="内容(JSON)">
          <el-input
            v-model="editorForm.content"
            type="textarea"
            :rows="10"
            class="font-mono"
          />
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input
            v-model="editorForm.reason"
            placeholder="记录本次变更原因，便于审计追溯"
          />
        </el-form-item>
      </el-form>
      <el-alert
        class="mb-2"
        type="warning"
        :closable="false"
        title="任何影响发布的配置变更都会创建新版本；发布计划开始时复制当前配置生成快照，后续修改不影响已执行的发布。"
      />
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSave">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 版本 Diff -->
    <el-dialog v-model="diffVisible" title="字段级版本对比" width="720px">
      <div class="flex items-center gap-2 mb-3">
        <el-select v-model="diffPair.v1" class="!w-28">
          <el-option
            v-for="v in versions"
            :key="v.version"
            :label="`V${v.version}`"
            :value="v.version"
          />
        </el-select>
        <span>→</span>
        <el-select v-model="diffPair.v2" class="!w-28">
          <el-option
            v-for="v in versions"
            :key="v.version"
            :label="`V${v.version}`"
            :value="v.version"
          />
        </el-select>
        <el-button size="small" @click="runDiff">重新对比</el-button>
      </div>
      <el-table
        v-loading="diffLoading"
        :data="diffItems"
        border
        empty-text="两个版本内容一致或无差异字段"
      >
        <el-table-column prop="field" label="字段" min-width="140" />
        <el-table-column label="当前值" min-width="200">
          <template #default="{ row }">
            <code class="text-xs">{{ JSON.stringify(row.oldValue) }}</code>
          </template>
        </el-table-column>
        <el-table-column label="新值" min-width="200">
          <template #default="{ row }">
            <code class="text-xs">{{ JSON.stringify(row.newValue) }}</code>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>
