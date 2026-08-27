/** 后端统一响应外壳（解包前） */
export interface ApiResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
}

/** 发布计划状态全集（ReleaseStatus，规范 §8/§44） */
export type ReleaseStatus =
  | "DRAFT"
  | "READY"
  | "TEST_MERGING"
  | "WAIT_CONFLICT_RESOLVE"
  | "TEST_DEPLOYING"
  | "TEST_DEPLOY_SUCCESS"
  | "WAIT_TEST_ACCEPT"
  | "TEST_REJECTED"
  | "TEST_ACCEPTED"
  | "RELEASE_BRANCH_CREATING"
  | "RELEASE_BRANCH_CREATED"
  | "PRE_DEPLOYING"
  | "PRE_DEPLOY_SUCCESS"
  | "PROD_DEPLOYING"
  | "PROD_DEPLOY_SUCCESS"
  | "WAIT_PROD_CONFIRM"
  | "COMPLETED"
  | "FAILED"
  | "TIMEOUT"
  | "CANCELLED";

/** 发布任务状态 */
export type ReleaseTaskStatus =
  | "PENDING"
  | "MERGING"
  | "MERGE_CONFLICT"
  | "BUILDING"
  | "DEPLOYING"
  | "SUCCESS"
  | "FAILED"
  | "TIMEOUT"
  | "CANCELLED";

/** 报警状态（规范 §59：Alerting / Acknowledged / Resolved） */
export type AlertStatus = "ALERTING" | "ACKNOWLEDGED" | "RESOLVED";

/** 项目类型（BACKEND / FRONTEND / FULLSTACK / MIXED） */
export type ProjectType = "BACKEND" | "FRONTEND" | "FULLSTACK" | "MIXED";

/** ISO 时间字符串或 null */
export type IsoTime = string | null;

/** 项目 GET /api/projects */
export interface Project {
  id: number;
  code: string;
  name: string;
  description?: string;
  projectType: ProjectType;
  ownerId?: number;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/** 项目成员 */
export interface ProjectMember {
  id: number;
  projectId: number;
  userId: number;
  role: string;
  createdAt?: string;
}

/** 项目服务 */
export interface ProjectService {
  id: number;
  projectId: number;
  code: string;
  name: string;
  /** BACKEND / FRONTEND */
  type: string;
  repositoryId?: number;
  enabled: boolean;
}

/** 需求 GET /api/projects/{projectId}/requirements */
export interface Requirement {
  id: number;
  projectId: number;
  title: string;
  description?: string;
  sourceType: string;
  externalId?: string;
  externalUrl?: string;
  ownerId?: number;
  priority?: string;
  status: string;
  createdAt?: string;
}

/** 发布计划 GET /api/release-plans */
export interface ReleasePlan {
  id: number;
  projectId: number;
  name: string;
  versionName?: string;
  description?: string;
  releaseOwnerId?: number;
  plannedTime?: string;
  status: ReleaseStatus;
  /** TEST / TEST,PRE,PROD 等 */
  environments: string;
  configSnapshotId?: number;
  createdBy?: number;
  createdAt?: string;
}

/** 报警 GET /api/alerts */
export interface Alert {
  id: number;
  projectId: number;
  projectKey: string;
  source: string;
  externalAlertId?: string;
  title: string;
  content?: string;
  /** CRITICAL / HIGH / MEDIUM / LOW */
  level?: string;
  status: AlertStatus;
  environment?: string;
  service?: string;
  labels?: string;
  fingerprint: string;
  notifiedRepeatCount: number;
  escalatedToLevel: number;
  firstOccurredAt?: IsoTime;
  lastOccurredAt?: IsoTime;
  ackedBy?: number;
  ackedAt?: IsoTime;
  resolvedAt?: IsoTime;
  updatedAt?: string;
}

/** 配置版本历史项 GET /api/admin/configs/{type}/{key}/versions */
export interface ConfigVersionItem {
  version: number;
  changedBy: number;
  reason: string;
}

/** 配置 diff 项 GET /api/admin/configs/{type}/{key}/diff?v1&v2 */
export interface ConfigDiffItem {
  field: string;
  oldValue: unknown;
  newValue: unknown;
}
