# ADR-005: 统一 Deployment Adapter 抽象

状态：Accepted | 日期：Phase 0

## 决策
`DeploymentAdapter` 接口统一后端与前端发布：

```
DeploymentAdapter
├── KubernetesAdapter   (Deployment/StatefulSet/DaemonSet/Job)
├── FrontendAdapter     (dist → Static/OSS)
├── OSSAdapter          (+ CDN Refresh)
├── CDNAdapter
├── ServerAdapter       (虚机/物理机)
└── CustomAdapter
```

验证统一走 `DeploymentVerifier.verify(deployment) → SUCCESS|FAILED|TIMEOUT|RUNNING`：
KubernetesDeploymentVerifier / StatefulSet / DaemonSet / Job / FrontendVerifier / CustomVerifier。

## 理由
前端发布不得强依赖 K8s Pod 判断；新增部署形态只加 Adapter 不改编排逻辑。
