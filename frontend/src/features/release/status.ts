import type { ReleaseStatus } from "@/api/types";

/** 状态中文标签 */
export const RELEASE_STATUS_LABEL: Record<ReleaseStatus, string> = {
  DRAFT: "草稿",
  READY: "待启动",
  TEST_MERGING: "测试合并中",
  WAIT_CONFLICT_RESOLVE: "等待解决合并冲突",
  TEST_DEPLOYING: "测试环境部署中",
  TEST_DEPLOY_SUCCESS: "测试部署成功",
  WAIT_TEST_ACCEPT: "等待测试验收",
  TEST_REJECTED: "验收驳回",
  TEST_ACCEPTED: "验收通过",
  RELEASE_BRANCH_CREATING: "Release Branch 创建中",
  RELEASE_BRANCH_CREATED: "Release Branch 已创建",
  PRE_DEPLOYING: "预发部署中",
  PRE_DEPLOY_SUCCESS: "预发部署成功",
  PROD_DEPLOYING: "生产部署中",
  PROD_DEPLOY_SUCCESS: "生产部署成功",
  WAIT_PROD_CONFIRM: "等待生产确认",
  COMPLETED: "已完成",
  FAILED: "失败",
  TIMEOUT: "超时",
  CANCELLED: "已取消"
};

/** Element Plus Tag 类型 */
export const RELEASE_STATUS_TAG_TYPE: Record<
  ReleaseStatus,
  "primary" | "success" | "warning" | "danger" | "info"
> = {
  DRAFT: "info",
  READY: "primary",
  TEST_MERGING: "warning",
  WAIT_CONFLICT_RESOLVE: "danger",
  TEST_DEPLOYING: "warning",
  TEST_DEPLOY_SUCCESS: "success",
  WAIT_TEST_ACCEPT: "warning",
  TEST_REJECTED: "danger",
  TEST_ACCEPTED: "success",
  RELEASE_BRANCH_CREATING: "warning",
  RELEASE_BRANCH_CREATED: "success",
  PRE_DEPLOYING: "warning",
  PRE_DEPLOY_SUCCESS: "success",
  PROD_DEPLOYING: "warning",
  PROD_DEPLOY_SUCCESS: "success",
  WAIT_PROD_CONFIRM: "warning",
  COMPLETED: "success",
  FAILED: "danger",
  TIMEOUT: "danger",
  CANCELLED: "info"
};

/** 主流程 Timeline 步骤定义（规范 §二十二：需求→代码→Merge→Jenkins→…→确认） */
export interface TimelineStep {
  key: string;
  title: string;
  /** 该步骤对应的"进行中"状态 */
  active: ReleaseStatus;
  /** 该步骤对应的"完成"状态集合（含后续所有状态，由工具函数判断） */
  done: ReleaseStatus[];
}

/** 全量状态顺序表（用于判断某状态是否已越过某步骤） */
export const STATUS_ORDER: ReleaseStatus[] = [
  "DRAFT",
  "READY",
  "TEST_MERGING",
  "WAIT_CONFLICT_RESOLVE",
  "TEST_DEPLOYING",
  "TEST_DEPLOY_SUCCESS",
  "WAIT_TEST_ACCEPT",
  "TEST_REJECTED",
  "TEST_ACCEPTED",
  "RELEASE_BRANCH_CREATING",
  "RELEASE_BRANCH_CREATED",
  "PRE_DEPLOYING",
  "PRE_DEPLOY_SUCCESS",
  "PROD_DEPLOYING",
  "PROD_DEPLOY_SUCCESS",
  "WAIT_PROD_CONFIRM",
  "COMPLETED"
];

export const TIMELINE_STEPS: TimelineStep[] = [
  {
    key: "draft",
    title: "创建计划 / 提交就绪",
    active: "DRAFT",
    done: ["DRAFT", "READY"]
  },
  {
    key: "merge",
    title: "Merge release_test（Jenkins 构建）",
    active: "TEST_MERGING",
    done: ["TEST_MERGING", "WAIT_CONFLICT_RESOLVE"]
  },
  {
    key: "test-deploy",
    title: "测试环境部署（Deployment/Health/Version Check）",
    active: "TEST_DEPLOYING",
    done: ["TEST_DEPLOYING", "TEST_DEPLOY_SUCCESS"]
  },
  {
    key: "test-accept",
    title: "测试验收",
    active: "WAIT_TEST_ACCEPT",
    done: ["WAIT_TEST_ACCEPT", "TEST_REJECTED", "TEST_ACCEPTED"]
  },
  {
    key: "release-branch",
    title: "创建 Release Branch",
    active: "RELEASE_BRANCH_CREATING",
    done: ["RELEASE_BRANCH_CREATING", "RELEASE_BRANCH_CREATED"]
  },
  { key: "pre", title: "预发发布", active: "PRE_DEPLOYING", done: [] },
  { key: "prod", title: "生产发布", active: "PROD_DEPLOYING", done: [] },
  {
    key: "confirm",
    title: "生产确认",
    active: "WAIT_PROD_CONFIRM",
    done: ["WAIT_PROD_CONFIRM"]
  },
  { key: "completed", title: "完成", active: "COMPLETED", done: ["COMPLETED"] }
];

/** 计算某状态在 Timeline 中步骤的展示类型：wait / process / finish / error */
export function stepType(
  status: ReleaseStatus
): "wait" | "process" | "finish" | "error" {
  if (
    status === "FAILED" ||
    status === "TIMEOUT" ||
    status === "TEST_REJECTED" ||
    status === "CANCELLED"
  ) {
    return "error";
  }
  const idx = STATUS_ORDER.indexOf(status);
  return idx >= STATUS_ORDER.indexOf("COMPLETED") ? "finish" : "process";
}
