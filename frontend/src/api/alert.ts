import { http } from "@/utils/http";
import type { Alert } from "./types";

/** GET /api/alerts?projectId= 报警列表 */
export const listAlerts = (projectId?: number) => {
  return http.request<Alert[]>(
    "get",
    "/api/alerts",
    projectId ? { params: { projectId } } : undefined
  );
};

/** POST /api/alerts/{id}/ack 确认（ACK 后停止普通重复通知，但升级继续） */
export const ackAlert = (id: number) => {
  return http.request<void>("post", `/api/alerts/${id}/ack`);
};

/** POST /api/alerts/{id}/resolve 恢复 */
export const resolveAlert = (id: number) => {
  return http.request<void>("post", `/api/alerts/${id}/resolve`);
};
