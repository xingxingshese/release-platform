/** 发布域 API（对齐 ReleaseController / PreProdReleaseService 契约）。 */
import { api } from './client'
import type { DeploymentNode } from '../types/deployment'
import type { ReleasePlan, ReleaseTask } from '../types/release'

export const releaseApi = {
  list: () => api.get<ReleasePlan[]>('/api/release-plans'),

  detail: (id: number) => api.get<ReleasePlan>(`/api/release-plans/${id}`),

  create: (body: { projectId: number; name: string; versionName?: string }) =>
    api.post<ReleasePlan>('/api/release-plans', body),

  ready: (id: number) => api.post<void>(`/api/release-plans/${id}/ready`),

  startTestRelease: (id: number) =>
    api.post<ReleaseTask>(`/api/release-plans/${id}/start`),

  accept: (id: number) => api.post<void>(`/api/release-plans/${id}/acceptance`, { decision: 'ACCEPT' }),

  reject: (id: number, reason: string) =>
    api.post<void>(`/api/release-plans/${id}/acceptance`, { decision: 'REJECT', reason }),

  createReleaseBranch: (id: number) =>
    api.post<void>(`/api/release-plans/${id}/release-branch`),

  deployPre: (id: number) => api.post<ReleaseTask>(`/api/release-plans/${id}/deploy-pre`),

  deployProd: (id: number) => api.post<ReleaseTask>(`/api/release-plans/${id}/deploy-prod`),

  confirm: (id: number) => api.post<void>(`/api/release-plans/${id}/confirm`),

  deploymentNodes: (taskId: number) =>
    api.get<DeploymentNode[]>(`/api/release-tasks/${taskId}/nodes`)
}
