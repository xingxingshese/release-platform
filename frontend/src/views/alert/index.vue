<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { Alert, AlertStatus } from "@/api/types";
import { ackAlert, listAlerts, resolveAlert } from "@/api/alert";

defineOptions({ name: "AlertList" });

const loading = ref(false);
const list = ref<Alert[]>([]);
const statusFilter = ref<AlertStatus | "">("");
const levelFilter = ref<string>("");
const POLL_MS = 10000;
let timer: ReturnType<typeof setInterval> | null = null;

const STATUS_META: Record<
  AlertStatus,
  { label: string; tag: "danger" | "warning" | "success" }
> = {
  ALERTING: { label: "报警中", tag: "danger" },
  ACKNOWLEDGED: { label: "已确认", tag: "warning" },
  RESOLVED: { label: "已恢复", tag: "success" }
};

async function load(silent = false) {
  if (!silent) loading.value = true;
  try {
    list.value = ((await listAlerts()) ?? [])
      .slice()
      .sort((a, b) => b.id - a.id);
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

const filtered = computed(() =>
  list.value.filter(
    x =>
      (!statusFilter.value || x.status === statusFilter.value) &&
      (!levelFilter.value || x.level === levelFilter.value)
  )
);

const alertingCount = computed(
  () => list.value.filter(x => x.status === "ALERTING").length
);

async function onAck(row: Alert) {
  await ElMessageBox.confirm(
    "确认该报警？ACK 后将停止普通重复通知，但超过升级时间仍未恢复将继续升级。",
    "ACK 确认",
    { type: "warning" }
  );
  await ackAlert(row.id);
  ElMessage.success("已确认");
  load();
}

async function onResolve(row: Alert) {
  await ElMessageBox.confirm("确认标记该报警已恢复？", "恢复确认", {
    type: "success"
  });
  await resolveAlert(row.id);
  ElMessage.success("已恢复");
  load();
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex justify-between items-center flex-wrap gap-2">
        <div class="flex items-center gap-3">
          <span>报警列表</span>
          <el-badge v-if="alertingCount" :value="alertingCount" type="danger">
            <el-tag type="info" size="small">活跃报警</el-tag>
          </el-badge>
        </div>
        <div class="flex items-center gap-2">
          <el-select
            v-model="statusFilter"
            placeholder="状态"
            clearable
            class="!w-32"
          >
            <el-option
              v-for="(meta, s) in STATUS_META"
              :key="s"
              :label="meta.label"
              :value="s"
            />
          </el-select>
          <el-select
            v-model="levelFilter"
            placeholder="级别"
            clearable
            class="!w-32"
          >
            <el-option label="CRITICAL" value="CRITICAL" />
            <el-option label="HIGH" value="HIGH" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="LOW" value="LOW" />
          </el-select>
          <el-button @click="load()">
            <el-icon><IEpRefresh /></el-icon>
          </el-button>
        </div>
      </div>
    </template>

    <el-table v-loading="loading" :data="filtered" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column
        prop="title"
        label="标题"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column prop="projectKey" label="项目" width="110" />
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag
            :type="
              row.level === 'CRITICAL' || row.level === 'HIGH'
                ? 'danger'
                : 'warning'
            "
            size="small"
          >
            {{ row.level ?? "-" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="STATUS_META[row.status as AlertStatus]?.tag ?? 'info'"
            size="small"
          >
            {{ STATUS_META[row.status as AlertStatus]?.label ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="environment" label="环境" width="90" />
      <el-table-column prop="notifiedRepeatCount" label="重复次数" width="90" />
      <el-table-column prop="escalatedToLevel" label="升级级别" width="90" />
      <el-table-column prop="firstOccurredAt" label="首次发生" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ALERTING'"
            link
            type="warning"
            @click="onAck(row)"
          >
            ACK
          </el-button>
          <el-button
            v-if="row.status !== 'RESOLVED'"
            link
            type="success"
            @click="onResolve(row)"
          >
            恢复
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-alert
      class="mt-4"
      type="info"
      :closable="false"
      title="规则：ACK ≠ 已解决。ACK 后停止普通重复通知；超过升级时间仍未恢复，升级通知继续（规范 §59）。列表每 10 秒自动刷新。"
    />
  </el-card>
</template>
