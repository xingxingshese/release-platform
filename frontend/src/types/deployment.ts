/** 发布部署节点（spec 011/012 契约对齐，对应 release_deployment_node 表）。 */

export interface DeploymentNode {
  id: number
  releaseTaskId: number
  serviceName: string
  nodeName: string
  deploymentType: 'K8S' | 'FRONTEND'
  replicaDesired: number | null
  replicaUpdated: number | null
  replicaReady: number | null
  replicaAvailable: number | null
  replicaUnavailable: number | null
  healthPassed: boolean | null
  versionExpected: string | null
  versionActual: string | null
  versionPassed: boolean | null
  result: 'SUCCESS' | 'RUNNING' | 'FAILED' | 'TIMEOUT' | 'VERSION_CHECK_FAILED'
  message: string | null
}

/** 发布 Timeline 步骤（规范 §三十一）。 */
export interface ReleaseTimelineStep {
  key: string
  label: string
  status: 'done' | 'active' | 'pending' | 'failed'
}
