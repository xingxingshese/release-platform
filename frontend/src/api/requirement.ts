import { http } from "@/utils/http";
import type { ApiResponse, Requirement } from "./types";

const base = (projectId: number) => `/api/projects/${projectId}/requirements`;

/** POST /api/projects/{projectId}/requirements 手动创建 */
export const createRequirement = (
  projectId: number,
  data: { title: string; description?: string; priority?: string }
) => {
  return http.request<ApiResponse<Requirement>>("post", base(projectId), {
    data
  });
};

/** GET /api/projects/{projectId}/requirements */
export const listRequirements = (projectId: number) => {
  return http.request<Requirement[]>("get", base(projectId));
};

/** POST .../import/{sourceType}/{externalId} 外部导入（幂等） */
export const importRequirement = (
  projectId: number,
  sourceType: string,
  externalId: string
) => {
  return http.request<ApiResponse<Requirement>>(
    "post",
    `${base(projectId)}/import/${sourceType}/${externalId}`
  );
};

/** GET .../external/{sourceType}?keyword= 外部需求搜索（Yunxiao Stub） */
export const searchExternalRequirements = (
  sourceType: string,
  keyword?: string
) => {
  return http.request<unknown[]>(
    "get",
    `/api/external/${sourceType}`,
    keyword ? { params: { keyword } } : undefined
  );
};
