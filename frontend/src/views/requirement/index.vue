<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import type { Requirement } from "@/api/types";
import {
  createRequirement,
  importRequirement,
  listRequirements
} from "@/api/requirement";

defineOptions({ name: "RequirementList" });

const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));

const loading = ref(false);
const list = ref<Requirement[]>([]);
const keyword = ref("");

async function load() {
  loading.value = true;
  try {
    list.value = (await listRequirements(projectId.value)) ?? [];
  } finally {
    loading.value = false;
  }
}
onMounted(load);

const filtered = computed(() =>
  keyword.value
    ? list.value.filter(x => x.title.includes(keyword.value))
    : list.value
);

// ---- 手动创建 ----
const createVisible = ref(false);
const createForm = reactive({ title: "", description: "", priority: "MEDIUM" });

function openCreate() {
  Object.assign(createForm, { title: "", description: "", priority: "MEDIUM" });
  createVisible.value = true;
}

async function submitCreate() {
  if (!createForm.title) {
    ElMessage.warning("请填写需求标题");
    return;
  }
  await createRequirement(projectId.value, { ...createForm });
  ElMessage.success("创建成功");
  createVisible.value = false;
  load();
}

// ---- 外部导入（云效 Stub，幂等） ----
const importExternalId = ref("");
const importing = ref(false);

async function submitImport() {
  if (!importExternalId.value) {
    ElMessage.warning("请填写外部需求ID");
    return;
  }
  importing.value = true;
  try {
    await importRequirement(projectId.value, "YUNXIAO", importExternalId.value);
    ElMessage.success("导入成功（重复导入自动幂等）");
    importExternalId.value = "";
    load();
  } finally {
    importing.value = false;
  }
}

const PRIORITY_TAG: Record<string, "danger" | "warning" | "info" | "primary"> =
  {
    HIGH: "danger",
    MEDIUM: "warning",
    LOW: "info"
  };
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex justify-between items-center flex-wrap gap-2">
        <span>需求管理（项目 #{{ projectId }}）</span>
        <div class="flex items-center gap-2">
          <el-input
            v-model="keyword"
            placeholder="搜索标题"
            clearable
            class="!w-48"
          />
          <el-button type="primary" @click="openCreate">新建需求</el-button>
        </div>
      </div>
    </template>

    <!-- 外部导入 -->
    <div class="flex items-center gap-2 mb-4">
      <el-input
        v-model="importExternalId"
        placeholder="外部需求ID（如云效 workitem-id）"
        class="!w-64"
      />
      <el-button :loading="importing" @click="submitImport">
        从云效导入
      </el-button>
    </div>

    <el-table v-loading="loading" :data="filtered" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column
        prop="title"
        label="标题"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column prop="sourceType" label="来源" width="120" />
      <el-table-column prop="externalId" label="外部ID" width="130" />
      <el-table-column label="优先级" width="100">
        <template #default="{ row }">
          <el-tag
            v-if="row.priority"
            :type="PRIORITY_TAG[row.priority] ?? 'info'"
            size="small"
          >
            {{ row.priority }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
    </el-table>

    <el-dialog v-model="createVisible" title="新建需求" width="480px">
      <el-form label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" class="w-full">
            <el-option label="HIGH" value="HIGH" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="LOW" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
