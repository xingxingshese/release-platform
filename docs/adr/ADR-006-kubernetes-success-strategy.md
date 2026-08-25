# ADR-006: K8s 部署成功判定策略

状态：Accepted | 日期：Phase 0

## 决策（核心红线）
**Jenkins SUCCESS ≠ 部署成功。** 最终成功必须：

```
Jenkins SUCCESS AND Deployment SUCCESS AND Health Check AND Version Check
=> ReleaseSuccessEvaluator => ReleaseTask SUCCESS => 才发"部署成功"通知
```

K8s Deployment 成功条件：

```
desired == updated && desired == ready && desired == available && unavailable == 0
```

任一 Pod CrashLoopBackOff / ImagePullBackOff / Pending / NotReady / Failed / 整体 Timeout ⇒ Deployment FAILED/TIMEOUT。滚动发布须等待旧版本实例按策略退出。每个 Pod 状态记录到 release_deployment_node。

## TDD 用例基线（先写测试）
1. 4/4 Ready → SUCCESS
2. 3/4 Ready → RUNNING
3. 4/4 Ready 但 unavailable=1 → FAILED
4. 出现 CrashLoopBackOff → FAILED
5. 超过 timeout → TIMEOUT
6. Version Check 版本不一致 → VERSION_CHECK_FAILED
