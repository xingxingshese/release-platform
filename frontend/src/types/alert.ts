/** 报警类型（spec 014 契约对齐）。 */

export type AlertStatus = 'ALERTING' | 'ACKNOWLEDGED' | 'RESOLVED'

export interface Alert {
  id: number
  projectId: number
  projectKey: string
  title: string
  content: string
  level: 'INFO' | 'WARN' | 'CRITICAL'
  environment: string
  service: string
  fingerprint: string
  status: AlertStatus
  count: number
  escalationLevel: number
  firstOccurredAt: string
  lastOccurredAt: string
}
