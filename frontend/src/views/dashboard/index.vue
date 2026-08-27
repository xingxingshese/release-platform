<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import type { Alert, Project, ReleasePlan } from "@/api/types";
import {
  RELEASE_STATUS_LABEL,
  RELEASE_STATUS_TAG_TYPE
} from "@/features/release/status";
import { listProjects } from "@/api/project";
import { listReleasePlans } from "@/api/release";
import { listAlerts } from "@/api/alert";

defineOptions({ name: "Dashboard" });

const router = useRouter();
const loading = ref(true);
const projects = ref<Project[]>([]);
const allPlans = ref<ReleasePlan[]>([]);
const plans = ref<ReleasePlan[]>([]);
const alerts = ref<Alert[]>([]);

const activePlans = computed(() =>
  plans.value.filter(p => !["COMPLETED", "CANCELLED"].includes(p.status))
);
const alertingCount = computed(
  () => alerts.value.filter(a => a.status === "ALERTING").length
);

async function load() {
  loading.value = true;
  try {
    const [p, r, a] = await Promise.all([
      listProjects().catch(() => []),
      listReleasePlans().catch(() => []),
      listAlerts().catch(() => [])
    ]);
    projects.value = p ?? [];
    allPlans.value = r ?? [];
    plans.value = allPlans.value
      .slice()
      .sort((x, y) => y.id - x.id)
      .slice(0, 8);
    alerts.value = (a ?? [])
      .filter(x => x.status !== "RESOLVED")
      .sort((x, y) => y.id - x.id)
      .slice(0, 8);
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col
        v-for="card in [
          {
            label: '项目总数',
            value: projects.length,
            icon: 'ep/folder-opened',
            color: '#409eff'
          },
          {
            label: '进行中的发布',
            value: activePlans.length,
            icon: 'ep/promotion',
            color: '#e6a23c'
          },
          {
            label: '发布计划总数',
            value: allPlans.length,
            icon: 'ep/list',
            color: '#67c23a'
          },
          {
            label: '未恢复报警',
            value: alertingCount,
            icon: 'ep/bell',
            color: '#f56c6c'
          }
        ]"
        :key="card.label"
        :xs="12"
        :sm="6"
      >
        <el-card shadow="hover">
          <div class="flex items-center gap-3">
            <el-icon :size="36" :color="card.color">
              <component :is="card.icon.replace('ep/', 'IEp')" />
            </el-icon>
            <div>
              <div class="text-sm text-gray-500">{{ card.label }}</div>
              <div class="text-2xl font-semibold">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-4">
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>
            <div class="flex justify-between items-center">
              <span>最近发布计划</span>
              <el-button
                link
                type="primary"
                @click="router.push('/release/list')"
              >
                查看全部
              </el-button>
            </div>
          </template>
          <el-table v-loading="loading" :data="plans" size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column
              prop="name"
              label="名称"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column prop="versionName" label="版本" width="100" />
            <el-table-column label="状态" width="150">
              <template #default="{ row }">
                <el-tag
                  :type="
                    RELEASE_STATUS_TAG_TYPE[
                      row.status as keyof typeof RELEASE_STATUS_TAG_TYPE
                    ]
                  "
                  size="small"
                >
                  {{
                    RELEASE_STATUS_LABEL[
                      row.status as keyof typeof RELEASE_STATUS_LABEL
                    ] ?? row.status
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="70">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="router.push(`/release/detail/${row.id}`)"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>
            <div class="flex justify-between items-center">
              <span>活跃报警</span>
              <el-button
                link
                type="primary"
                @click="router.push('/alert/list')"
              >
                报警中心
              </el-button>
            </div>
          </template>
          <el-empty
            v-if="!alerts.length"
            description="暂无活跃报警"
            :image-size="60"
          />
          <el-table v-else v-loading="loading" :data="alerts" size="small">
            <el-table-column
              prop="title"
              label="标题"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column prop="level" label="级别" width="80" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column
              prop="notifiedRepeatCount"
              label="重复次数"
              width="80"
            />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
