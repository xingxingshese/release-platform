import { http } from "@/utils/http";
import type { ApiResponse, ReleasePlan } from "./types";

/** POST /api/release-plans 创建（DRAFT） */
export const createReleasePlan = (data: {
  projectId: number;
  name: string;
  versionName?: string;
  description?: string;
  environments?: string;
}) => {
  return http.request<ApiResponse<ReleasePlan>>("post", "/api/release-plans", {
    data
  });
};

/** GET /api/release-plans */
export const listReleasePlans = () => {
  return http.request<ReleasePlan[]>("get", "/api/release-plans");
};

/** GET /api/release-plans/{id} */
export const getReleasePlan = (id: number) => {
  return http.request<ReleasePlan>("get", `/api/release-plans/${id}`);
};

/** POST /api/release-plans/{id}/ready 提交就绪 DRAFT→READY */
export const readyReleasePlan = (id: number) => {
  return http.request<void>("post", `/api/release-plans/${id}/ready`);
};

/** POST /api/release-plans/{id}/test-release 启动测试发布（分布式锁防重复） */
export const startTestRelease = (id: number) => {
  return http.request<unknown>("post", `/api/release-plans/${id}/test-release`);
};

/** POST .../test-accept 测试验收通过 */
export const acceptTest = (id: number) => {
  return http.request<void>("post", `/api/release-plans/${id}/test-accept`);
};

/** POST .../test-reject 测试验收驳回 */
export const rejectTest = (id: number) => {
  return http.request<void>("post", `/api/release-plans/${id}/test-reject`);
};

/** POST .../create-release-branch 创建 Release Branch */
export const createReleaseBranch = (id: number) => {
  return http.request<void>(
    "post",
    `/api/release-plans/${id}/create-release-branch`
  );
};

/** POST .../prod-confirm 生产确认 WAIT_PROD_CONFIRM→COMPLETED */
export const confirmProduction = (id: number) => {
  return http.request<void>("post", `/api/release-plans/${id}/prod-confirm`);
};
