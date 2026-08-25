/** 需求域 API（spec 002 契约对齐）。 */
import { api } from './client'
import type { Project } from '../types/project'

export interface Requirement {
  id: number
  projectId: number
  title: string
  source: 'MANUAL' | 'YUNXIAO'
  externalKey: string | null
  status: string
}

export const projectApi = {
  list: () => api.get<Project[]>('/api/projects'),
  create: (body: { name: string; code: string; projectType: Project['projectType'] }) =>
    api.post<Project>('/api/projects', body),
  requirements: (projectId: number) => api.get<Requirement[]>(`/api/requirements?projectId=${projectId}`)
}
