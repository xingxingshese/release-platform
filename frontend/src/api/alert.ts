/** 报警域 API（spec 014 契约对齐）。 */
import { api } from './client'
import type { Alert } from '../types/alert'

export const alertApi = {
  list: (projectId?: number) =>
    api.get<Alert[]>(projectId ? `/api/alerts?projectId=${projectId}` : '/api/alerts'),

  ack: (id: number) => api.post<void>(`/api/alerts/${id}/ack`),

  resolve: (id: number) => api.post<void>(`/api/alerts/${id}/resolve`)
}
