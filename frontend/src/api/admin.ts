/** 管理员配置中心 API（spec 016 契约对齐）。 */
import { api } from './client'
import type { ConfigDiffItem, ConfigVersionSummary, Project } from '../types/project'

export const adminApi = {
  configVersions: (type: string, key: string) =>
    api.get<ConfigVersionSummary[]>(`/api/admin/configs/${type}/${key}/versions`),

  saveConfig: (type: string, key: string, content: string, reason: string) =>
    api.post<{ version: number }>(`/api/admin/configs/${type}/${key}/versions`, { content, reason }),

  configDiff: (type: string, key: string, v1: number, v2: number) =>
    api.get<ConfigDiffItem[]>(`/api/admin/configs/${type}/${key}/diff?v1=${v1}&v2=${v2}`),

  projects: () => api.get<Project[]>('/api/projects')
}
