/** 项目与配置中心类型（spec 001 / 016 契约对齐）。 */

export interface Project {
  id: number
  name: string
  code: string
  projectType: 'BACKEND' | 'FRONTEND' | 'FULLSTACK' | 'MIXED'
}

export interface ConfigVersionSummary {
  version: number
  changedBy: number
  reason: string
}

export interface ConfigDiffItem {
  path: string
  before: string | null
  after: string | null
}
