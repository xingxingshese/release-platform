<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { ReleasePlan } from "@/api/types";
import {
  RELEASE_STATUS_LABEL,
  RELEASE_STATUS_TAG_TYPE,
  TIMELINE_STEPS,
  stepType
} from "@/features/release/status";
import { getReleasePlan } from "@/api/release";

defineOptions({ name: "ReleaseDetail" });

const route = useRoute();
const router = useRouter();
const plan = ref<ReleasePlan | null>(null);
const loading = ref(false);
const POLL_MS = 5000;
let timer: ReturnType<typeof setInterval> | null = null;

async function load(silent = false) {
  if (!silent) loading.value = true;
  try {
    plan.value = await getReleasePlan(Number(route.params.id));
  } catch {
    if (!silent) ElMessage.error("加载发布详情失败");
  } finally {
    if (!silent) loading.value = false;
  }
}

onMounted(() => {
  load();
  timer = setInterval(() => load(true), POLL_MS);
});
onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
});

const status = computed(
  () => (plan.value?.status ?? "DRAFT") as ReleasePlan["status"]
);

/** Timeline 每一步的展示类型 */
function typeOf(stepKey: string): "wait" | "process" | "finish" | "error" {
  const s = status.value;
  if (
    s === "FAILED" ||
    s === "TIMEOUT" ||
    s === "TEST_REJECTED" ||
    s === "CANCELLED"
  ) {
    // 失败态：定位到失败步骤标红，其后保持 wait
    const failedKeys: Record<string, string[]> = {
      FAILED: ["merge", "test-deploy", "release-branch", "pre", "prod"],
      TIMEOUT: ["merge", "test-deploy", "pre", "prod"],
      TEST_REJECTED: ["test-accept"],
      CANCELLED: []
    };
    const order = TIMELINE_STEPS.map(x => x.key);
    const currentIdx = order.indexOf(currentStepKey.value);
    const idx = order.indexOf(stepKey);
    return failedKeys[s]?.includes(stepKey) && idx === currentIdx
      ? "error"
      : idx < currentIdx
        ? "finish"
        : "wait";
  }
  const currentIdx = TIMELINE_STEPS.findIndex(
    x => x.key === currentStepKey.value
  );
  const idx = TIMELINE_STEPS.findIndex(x => x.key === stepKey);
  if (idx < currentIdx) return "finish";
  if (idx === currentIdx) {
    // 当前步骤若处于完成态状态则显示 finish（如 TEST_DEPLOY_SUCCESS）
    const step = TIMELINE_STEPS[idx];
    return s === step.active ? "process" : "finish";
  }
  return "wait";
}

/** 根据当前状态推断所处步骤 */
const currentStepKey = computed(() => {
  const s = status.value;
  const map: Record<string, string> = {
    DRAFT: "draft",
    READY: "draft",
    TEST_MERGING: "merge",
    WAIT_CONFLICT_RESOLVE: "merge",
    TEST_DEPLOYING: "test-deploy",
    TEST_DEPLOY_SUCCESS: "test-deploy",
    WAIT_TEST_ACCEPT: "test-accept",
    TEST_REJECTED: "test-accept",
    TEST_ACCEPTED: "test-accept",
    RELEASE_BRANCH_CREATING: "release-branch",
    RELEASE_BRANCH_CREATED: "release-branch",
    PRE_DEPLOYING: "pre",
    PRE_DEPLOY_SUCCESS: "pre",
    PROD_DEPLOYING: "prod",
    PROD_DEPLOY_SUCCESS: "prod",
    WAIT_PROD_CONFIRM: "confirm",
    COMPLETED: "completed"
  };
  return map[s] ?? "draft";
});

const isFailed = computed(() =>
  ["FAILED", "TIMEOUT", "TEST_REJECTED", "CANCELLED"].includes(status.value)
);

async function runAction(
  fn: () => Promise<unknown>,
  label: string,
  confirm = true
) {
  if (confirm) {
    await ElMessageBox.confirm(`确认执行「${label}」？`, "操作确认", {
      type: "warning"
    });
  }
  await fn();
  ElMessage.success(`${label} 成功`);
  load();
}

const actions = computed<
  Array<{
    label: string;
    run: () => Promise<unknown>;
    type: "primary" | "success" | "danger";
  }>
>(() => {
  const id = Number(route.params.id);
  switch (status.value) {
    case "DRAFT":
      return [
        {
          label: "提交就绪",
          run: () => import("@/api/release").then(m => m.readyReleasePlan(id)),
          type: "primary"
        }
      ];
    case "READY":
      return [
        {
          label: "启动测试发布",
          run: () => import("@/api/release").then(m => m.startTestRelease(id)),
          type: "primary"
        }
      ];
    case "WAIT_TEST_ACCEPT":
      return [
        {
          label: "验收通过",
          run: () => import("@/api/release").then(m => m.acceptTest(id)),
          type: "success"
        },
        {
          label: "验收驳回",
          run: () => import("@/api/release").then(m => m.rejectTest(id)),
          type: "danger"
        }
      ];
    case "TEST_ACCEPTED":
      return [
        {
          label: "创建 Release Branch",
          run: () =>
            import("@/api/release").then(m => m.createReleaseBranch(id)),
          type: "primary"
        }
      ];
    case "WAIT_PROD_CONFIRM":
      return [
        {
          label: "生产确认",
          run: () => import("@/api/release").then(m => m.confirmProduction(id)),
          type: "success"
        }
      ];
    default:
      return [];
  }
});
</script>

<template>
  <el-card v-loading="loading" shadow="never">
    <template #header>
      <div class="flex justify-between items-center flex-wrap gap-2">
        <div class="flex items-center gap-3">
          <el-button link @click="router.back()">
            <el-icon><IEpArrowLeft /></el-icon>
          </el-button>
          <span class="font-semibold">
            {{ plan?.name ?? "发布详情" }}
            <span v-if="plan?.versionName" class="text-gray-400 ml-1">
              {{ plan.versionName }}
            </span>
          </span>
          <el-tag :type="RELEASE_STATUS_TAG_TYPE[status]">
            {{ RELEASE_STATUS_LABEL[status] ?? status }}
          </el-tag>
        </div>
        <div>
          <el-button
            v-for="a in actions"
            :key="a.label"
            :type="a.type"
            size="small"
            @click="runAction(a.run, a.label)"
          >
            {{ a.label }}
          </el-button>
          <el-tooltip content="每 5 秒自动刷新">
            <el-button link>
              <el-icon><IEpRefresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
    </template>

    <el-alert
      v-if="status === 'WAIT_CONFLICT_RESOLVE'"
      title="Merge 冲突：已进入等待解决冲突状态，禁止自动绕过冲突。请在代码仓库解决冲突后重新发起测试发布。"
      type="error"
      show-icon
      :closable="false"
      class="mb-4"
    />
    <el-alert
      v-if="isFailed"
      title="该发布计划处于终态失败/中止状态"
      type="warning"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <el-steps direction="vertical" :active="99" class="max-w-3xl">
      <el-step
        v-for="step in TIMELINE_STEPS"
        :key="step.key"
        :title="step.title"
        :status="typeOf(step.key)"
        :description="
          step.key === currentStepKey
            ? `当前状态：${RELEASE_STATUS_LABEL[status] ?? status}`
            : ''
        "
      >
        <template v-if="typeOf(step.key) !== 'wait'" #icon>
          <el-icon v-if="typeOf(step.key) === 'error'" color="#f56c6c"
            ><IEpCircleCloseFilled
          /></el-icon>
          <el-icon v-else-if="typeOf(step.key) === 'finish'" color="#67c23a"
            ><IEpSuccessFilled
          /></el-icon>
          <el-icon v-else color="#e6a23c" class="animate-spin"
            ><IEpLoading
          /></el-icon>
        </template>
      </el-step>
    </el-steps>

    <el-descriptions
      v-if="plan"
      :column="2"
      border
      class="mt-6 max-w-3xl"
      size="small"
    >
      <el-descriptions-item label="计划 ID">{{ plan.id }}</el-descriptions-item>
      <el-descriptions-item label="项目 ID">{{
        plan.projectId
      }}</el-descriptions-item>
      <el-descriptions-item label="环境范围">{{
        plan.environments
      }}</el-descriptions-item>
      <el-descriptions-item label="配置快照">{{
        plan.configSnapshotId ?? "-"
      }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{
        plan.createdAt ?? "-"
      }}</el-descriptions-item>
      <el-descriptions-item label="计划时间">{{
        plan.plannedTime ?? "-"
      }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      class="mt-4 max-w-3xl"
      type="info"
      :closable="false"
      title="说明：部署/Pod 级节点明细需要后端补充查询接口（当前 /release-plans/{id} 仅返回计划本体）"
    />
  </el-card>
</template>
