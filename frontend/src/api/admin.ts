import { http } from "@/utils/http";
import type { ConfigDiffItem, ConfigVersionItem } from "./types";

const base = (type: string, key: string) =>
  `/api/admin/configs/${type}/${encodeURIComponent(key)}`;

/**
 * POST /api/admin/configs/{type}/{key}/versions
 * 保存配置新版本（版本号单调递增；任何影响发布的配置变更必须创建新版本）
 */
export const saveConfigVersion = (
  type: string,
  key: string,
  data: { content: string; reason?: string }
) => {
  return http.request<{ id: number; version: number; changedBy: number }>(
    "post",
    `${base(type, key)}/versions`,
    { data }
  );
};

/** GET /api/admin/configs/{type}/{key}/versions 版本历史（新→旧） */
export const listConfigVersions = (type: string, key: string) => {
  return http.request<ConfigVersionItem[]>(
    "get",
    `${base(type, key)}/versions`
  );
};

/** GET /api/admin/configs/{type}/{key}/diff?v1&v2 字段级对比 当前值 vs 新值 */
export const diffConfigVersions = (
  type: string,
  key: string,
  v1: number,
  v2: number
) => {
  return http.request<ConfigDiffItem[]>("get", `${base(type, key)}/diff`, {
    params: { v1, v2 }
  });
};
