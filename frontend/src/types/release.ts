/** 发布计划类型（与后端 API 契约对齐）。 */

export type ReleaseStatus =
  | 'DRAFT' | 'READY' | 'TEST_MERGING' | 'WAIT_CONFLICT_RESOLVE'
  | 'TEST_DEPLOYING' | 'TEST_DEPLOY_SUCCESS' | 'WAIT_TEST_ACCEPT'
  | 'TEST_REJECTED' | 'TEST_ACCEPTED' | 'RELEASE_BRANCH_CREATING'
  | 'RELEASE_BRANCH_CREATED' | 'PRE_DEPLOYING' | 'PRE_DEPLOY_SUCCESS'
  | 'PROD_DEPLOYING' | 'PROD_DEPLOY_SUCCESS' | 'WAIT_PROD_CONFIRM'
  | 'COMPLETED' | 'FAILED' | 'TIMEOUT' | 'CANCELLED'

export interface ReleasePlan {
  id: number
  projectId: number
  name: string
  versionName: string | null
  status: ReleaseStatus
  environments: string
}

export interface ReleaseTask {
  id: number
  releasePlanId: number
  environmentCode: string
  status: string
  jenkinsBuildNumber: number | null
}
