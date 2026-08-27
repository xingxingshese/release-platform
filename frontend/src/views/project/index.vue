<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import type {
  Project,
  ProjectMember,
  ProjectService,
  ProjectType
} from "@/api/types";
import {
  addProjectMember,
  addProjectService,
  createProject,
  listProjectMembers,
  listProjectServices,
  listProjects
} from "@/api/project";

defineOptions({ name: "ProjectList" });

const router = useRouter();
const loading = ref(false);
const projects = ref<Project[]>([]);

const PROJECT_TYPES: Array<{ value: ProjectType; label: string }> = [
  { value: "BACKEND", label: "后端" },
  { value: "FRONTEND", label: "前端" },
  { value: "FULLSTACK", label: "全栈" },
  { value: "MIXED", label: "混合" }
];

async function load() {
  loading.value = true;
  try {
    projects.value = (await listProjects()) ?? [];
  } finally {
    loading.value = false;
  }
}
onMounted(load);

// ---- 新建项目 ----
const createVisible = ref(false);
const createForm = reactive({
  code: "",
  name: "",
  description: "",
  projectType: "BACKEND" as ProjectType
});

function openCreate() {
  Object.assign(createForm, {
    code: "",
    name: "",
    description: "",
    projectType: "BACKEND"
  });
  createVisible.value = true;
}

async function submitCreate() {
  if (!createForm.code || !createForm.name) {
    ElMessage.warning("请填写项目编码与名称");
    return;
  }
  await createProject({
    code: createForm.code,
    name: createForm.name,
    description: createForm.description || undefined,
    projectType: createForm.projectType
  });
  ElMessage.success("创建成功");
  createVisible.value = false;
  load();
}

// ---- 项目详情抽屉：成员 + 服务 ----
const drawerVisible = ref(false);
const current = ref<Project | null>(null);
const members = ref<ProjectMember[]>([]);
const services = ref<ProjectService[]>([]);

async function openDetail(row: Project) {
  current.value = row;
  drawerVisible.value = true;
  [members.value, services.value] = await Promise.all([
    listProjectMembers(row.id).catch(() => []),
    listProjectServices(row.id).catch(() => [])
  ]);
}

const memberForm = reactive({
  userId: undefined as number | undefined,
  role: ""
});
async function submitMember() {
  if (!current.value || !memberForm.userId || !memberForm.role) {
    ElMessage.warning("请填写用户ID与角色");
    return;
  }
  await addProjectMember(current.value.id, {
    userId: memberForm.userId,
    role: memberForm.role
  });
  ElMessage.success("成员已添加");
  memberForm.userId = undefined;
  memberForm.role = "";
  members.value = (await listProjectMembers(current.value.id)) ?? [];
}

const serviceForm = reactive({ code: "", name: "", type: "BACKEND" });
async function submitService() {
  if (!current.value || !serviceForm.code || !serviceForm.name) {
    ElMessage.warning("请填写服务编码与名称");
    return;
  }
  await addProjectService(current.value.id, { ...serviceForm });
  ElMessage.success("服务已添加");
  serviceForm.code = "";
  serviceForm.name = "";
  services.value = (await listProjectServices(current.value.id)) ?? [];
}

function typeLabel(t: string) {
  return PROJECT_TYPES.find(x => x.value === t)?.label ?? t;
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex justify-between items-center">
        <span>项目列表</span>
        <el-button type="primary" @click="openCreate">新建项目</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="projects" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="code" label="编码" width="140" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column
        prop="description"
        label="描述"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.projectType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? "是" : "否" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)"
            >成员/服务</el-button
          >
          <el-button
            link
            type="primary"
            @click="router.push(`/requirement/${row.id}`)"
          >
            需求管理
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建项目 -->
    <el-dialog v-model="createVisible" title="新建项目" width="480px">
      <el-form label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="createForm.code" placeholder="如：order-service" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="createForm.projectType" class="w-full">
            <el-option
              v-for="t in PROJECT_TYPES"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
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

    <!-- 成员与服务 -->
    <el-drawer
      v-model="drawerVisible"
      :title="`项目详情 - ${current?.name ?? ''}`"
      size="560px"
    >
      <template v-if="current">
        <h4 class="mb-2 font-semibold">成员（六类负责人）</h4>
        <el-table :data="members" size="small" border class="mb-3">
          <el-table-column prop="userId" label="用户ID" width="90" />
          <el-table-column prop="role" label="角色" />
        </el-table>
        <div class="flex gap-2 mb-6">
          <el-input-number
            v-model="memberForm.userId"
            placeholder="用户ID"
            controls-position="right"
            class="!w-32"
          />
          <el-input
            v-model="memberForm.role"
            placeholder="角色，如 DEV_OWNER"
            class="!w-48"
          />
          <el-button type="primary" @click="submitMember">添加成员</el-button>
        </div>

        <h4 class="mb-2 font-semibold">服务列表</h4>
        <el-table :data="services" size="small" border class="mb-3">
          <el-table-column prop="code" label="编码" width="130" />
          <el-table-column prop="name" label="名称" width="130" />
          <el-table-column prop="type" label="类型" width="90" />
        </el-table>
        <div class="flex gap-2">
          <el-input
            v-model="serviceForm.code"
            placeholder="编码"
            class="!w-32"
          />
          <el-input
            v-model="serviceForm.name"
            placeholder="名称"
            class="!w-36"
          />
          <el-select v-model="serviceForm.type" class="!w-28">
            <el-option label="后端" value="BACKEND" />
            <el-option label="前端" value="FRONTEND" />
          </el-select>
          <el-button type="primary" @click="submitService">添加服务</el-button>
        </div>
      </template>
    </el-drawer>
  </el-card>
</template>
