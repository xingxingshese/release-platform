<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { Project, ReleasePlan } from "@/api/types";
import {
  RELEASE_STATUS_LABEL,
  RELEASE_STATUS_TAG_TYPE
} from "@/features/release/status";
import { listProjects } from "@/api/project";
import {
  acceptTest,
  confirmProduction,
  createReleaseBranch,
  createReleasePlan,
  listReleasePlans,
  readyReleasePlan,
  rejectTest,
  startTestRelease
} from "@/api/release";

defineOptions({ name: "ReleaseList" });

const router = useRouter();
const loading = ref(false);
const plans = ref<ReleasePlan[]>([]);
const projects = ref<Project[]>([]);

/** 各状态下可用的操作（与后端状态机守卫一致，前端仅做引导，后端为准） */
const ACTIONS: Record<
  string,
  Array<{
    key: string;
    label: string;
    type?: "primary" | "success" | "danger" | "warning";
  }>
> = {
  DRAFT: [{ key: "ready", label: "提交就绪" }],
  READY: [{ key: "test-release", label: "启动测试发布", type: "primary" }],
  WAIT_TEST_ACCEPT: [
    { key: "accept", label: "验收通过", type: "success" },
    { key: "reject", label: "验收驳回", type: "danger" }
  ],
  TEST_ACCEPTED: [
    { key: "branch", label: "创建 Release Branch", type: "primary" }
  ],
  WAIT_PROD_CONFIRM: [{ key: "confirm", label: "生产确认", type: "success" }]
};

async function load() {
  loading.value = true;
  try {
    const [p, r] = await Promise.all([
      listProjects().catch(() => []),
      listReleasePlans()
    ]);
    projects.value = p ?? [];
    plans.value = (r ?? []).slice().sort((x, y) => y.id - x.id);
  } finally {
    loading.value = false;
  }
}

onMounted(load);

// ---- 创建发布计划 ----
const createVisible = ref(false);
const createForm = reactive({
  projectId: undefined as number | undefined,
  name: "",
  versionName: "",
  description: "",
  envList: ["TEST"] as string[]
});

function openCreate() {
  Object.assign(createForm, {
    projectId: undefined,
    name: "",
    versionName: "",
    description: "",
    envList: ["TEST"]
  });
  createVisible.value = true;
}

async function submitCreate() {
  if (!createForm.projectId || !createForm.name) {
    ElMessage.warning("请选择项目并填写计划名称");
    return;
  }
  await createReleasePlan({
    projectId: createForm.projectId,
    name: createForm.name,
    versionName: createForm.versionName || undefined,
    description: createForm.description || undefined,
    environments: createForm.envList.join(",")
  });
  ElMessage.success("创建成功");
  createVisible.value = false;
  load();
}

// ---- 状态操作 ----
async function runAction(row: ReleasePlan, key: string) {
  const label = ACTIONS[row.status]?.find(a => a.key === key)?.label ?? key;
  await ElMessageBox.confirm(`确认执行「${label}」？`, "操作确认", {
    type: "warning"
  });
  switch (key) {
    case "ready":
      await readyReleasePlan(row.id);
      break;
    case "test-release":
      await startTestRelease(row.id);
      break;
    case "accept":
      await acceptTest(row.id);
      break;
    case "reject":
      await rejectTest(row.id);
      break;
    case "branch":
      await createReleaseBranch(row.id);
      break;
    case "confirm":
      await confirmProduction(row.id);
      break;
  }
  ElMessage.success(`${label} 成功`);
  load();
}

function projectName(id: number) {
  return projects.value.find(p => p.id === id)?.name ?? id;
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex justify-between items-center">
        <span>发布计划</span>
        <el-button type="primary" @click="openCreate">新建发布计划</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="plans" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="所属项目" width="140">
        <template #default="{ row }">{{ projectName(row.projectId) }}</template>
      </el-table-column>
      <el-table-column
        prop="name"
        label="名称"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column prop="versionName" label="版本" width="110" />
      <el-table-column prop="environments" label="环境" width="120" />
      <el-table-column label="状态" width="170">
        <template #default="{ row }">
          <el-tag
            :type="
              RELEASE_STATUS_TAG_TYPE[
                row.status as keyof typeof RELEASE_STATUS_TAG_TYPE
              ]
            "
          >
            {{
              RELEASE_STATUS_LABEL[
                row.status as keyof typeof RELEASE_STATUS_LABEL
              ] ?? row.status
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="router.push(`/release/detail/${row.id}`)"
          >
            详情
          </el-button>
          <el-button
            v-for="action in ACTIONS[row.status] ?? []"
            :key="action.key"
            link
            :type="action.type ?? 'primary'"
            @click="runAction(row, action.key)"
          >
            {{ action.label }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="新建发布计划" width="520px">
      <el-form label-width="90px">
        <el-form-item label="项目" required>
          <el-select
            v-model="createForm.projectId"
            placeholder="选择项目"
            class="w-full"
          >
            <el-option
              v-for="p in projects"
              :key="p.id"
              :label="`${p.name} (${p.code})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input
            v-model="createForm.name"
            placeholder="如：订单服务 8 月第 4 周发布"
          />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="createForm.versionName" placeholder="如：v1.4.0" />
        </el-form-item>
        <el-form-item label="环境范围">
          <el-checkbox-group v-model="createForm.envList">
            <el-checkbox value="TEST">测试</el-checkbox>
            <el-checkbox value="PRE">预发</el-checkbox>
            <el-checkbox value="PROD">生产</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="2"
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
