# Requirements — Kubernetes 部署验证

## 用户故事

- 作为发布工程师，我需要平台逐实例核验 K8s 滚动结果，任何 Pod 异常即整体判败。

## 业务规则

- 成功条件：desired==updated==ready==available 且 unavailable==0 且全部 Pod Ready(ADR-006)。
- CrashLoopBackOff/ImagePullBackOff/Pending/NotReady → FAILED；超时 → TIMEOUT；Version 不一致 → VERSION_CHECK_FAILED。
- Jenkins SUCCESS 仅是必要条件之一(红线 §五)。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- K8s API 不可达 → EXTERNAL_SERVICE_ERROR(任务 FAILED, 不误报成功)

## 权限

- `deployment:read 查看节点详情`

## 验收标准（摘要）

- ADR-006 六基线用例全绿
- node 表逐实例可追溯
