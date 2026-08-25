# 发布管理与统一报警平台——AI Coding 技术设计与实现规范

## 1. 项目目标

实现一个企业内部的**发布管理与统一报警平台**，用于统一管理：

- 发布计划
- 需求
- Git 代码仓库与开发分支
- 测试、预发、生产环境发布
- Jenkins 构建与部署
- Kubernetes 多实例部署状态
- 前端项目发布
- 发布验收与确认
- 企业微信、飞书等消息通知
- 外部系统报警接入
- 报警路由、频率控制、ACK、重复报警抑制
- 报警升级
- 管理员配置
- 权限
- 操作日志
- 配置版本与发布配置快照

系统的核心设计原则：

> **发布平台负责流程编排、权限、状态机、审计和最终发布结果判断；Jenkins 负责 CI/CD 执行；Kubernetes/OSS/CDN 等负责具体部署；通知中心负责消息发送；报警中心负责统一接入、路由、抑制和升级。**

系统不能将项目类型、环境、Jenkins Job、Git 分支、健康检查、通知规则、报警升级等硬编码在业务代码中，相关内容必须支持管理员配置。

---

# 2. 总体架构

```text
                           Web 前端
                              │
                              ▼
                         API / Gateway
                              │
                 ┌────────────┴────────────┐
                 │                         │
                 ▼                         ▼
             发布管理模块              报警管理模块
                 │                         │
       ┌─────────┼─────────┐       ┌──────┼──────┐
       │         │         │       │      │      │
       ▼         ▼         ▼       ▼      ▼      ▼
      Git      Jenkins     部署    Webhook 路由  升级
       │         │         │       │      │      │
       │         │     ┌───┴───┐   │      │      │
       │         │     ▼       ▼   │      │      │
       │         │    K8s    前端  │      │      │
       │         │            │   │      │      │
       └─────────┴────────────┴───┴──────┴──────┘
                              │
                       Notification
                              │
               ┌──────────────┼──────────────┐
               ▼              ▼              ▼
            企业微信          飞书           邮件
```

建议第一期采用模块化单体架构，不要过早拆分微服务：

```text
Frontend
  ↓
Spring Boot
  ↓
PostgreSQL / MySQL
Redis
MQ
```

后续可以根据规模拆分：

```text
release-service
alert-service
notification-service
deployment-service
config-service
```

---

# 3. 核心模块

系统至少包含以下模块：

```text
1. 首页 Dashboard
2. 发布计划
3. 需求管理
4. 代码仓库管理
5. 环境管理
6. Jenkins 管理
7. 部署管理
8. 测试验收
9. 发布确认
10. 消息通知
11. 报警管理
12. 项目管理
13. 权限管理
14. 系统配置
15. 操作日志
16. 配置版本
```

---

# 4. 项目模型

管理员可以创建项目。

## 4.1 项目字段

```text
Project
- id
- code
- name
- description
- project_type
- owner_id
- enabled
- created_at
- updated_at
```

`project_type`：

```text
BACKEND
FRONTEND
FULLSTACK
MIXED
```

项目支持多个服务：

```text
Project
 ├── Service
 ├── Service
 └── Service
```

Service：

```text
Service
- id
- project_id
- code
- name
- type
- repository_id
- enabled
```

Service 类型：

```text
BACKEND
FRONTEND
OTHER
```

---

# 5. 项目成员

项目管理员可以配置：

```text
项目负责人
开发负责人
测试负责人
产品负责人
发布负责人
报警负责人
```

同时支持具体成员：

```text
ProjectMember
- project_id
- user_id
- role
```

角色：

```text
PROJECT_OWNER
DEVELOPER
TESTER
PRODUCT
RELEASE_OWNER
ALERT_OWNER
```

同一个角色允许多人。

---

# 6. 需求管理

发布计划支持三种需求来源。

## 6.1 手动创建

```text
Requirement
- id
- project_id
- title
- description
- source_type
- external_id
- external_url
- owner_id
- priority
- status
- created_at
- updated_at
```

`source_type`：

```text
MANUAL
YUNXIAO
JIRA
TAPD
OTHER
```

## 6.2 云效导入

设计 Provider：

```text
RequirementProvider
 ├── YunxiaoProvider
 ├── JiraProvider
 ├── TapdProvider
 └── CustomProvider
```

Provider 至少支持：

```text
search()
getDetail()
import()
```

## 6.3 发布计划关联需求

```text
ReleasePlanRequirement
- release_plan_id
- requirement_id
```

一个发布计划支持多个需求。

---

# 7. Git 仓库

Git 必须采用 Provider 抽象。

```text
GitProvider
 ├── GitLab
 ├── GitHub
 ├── Gitee
 ├── Codeup
 └── Custom
```

统一接口：

```text
getRepository()
getBranches()
getBranch()
getLatestCommit()
createBranch()
mergeBranch()
checkMerge()
getDiff()
```

Repository：

```text
Repository
- id
- project_id
- name
- url
- provider_type
- credential_id
- default_branch
- enabled
```

---

# 8. 发布计划

发布计划是整个系统核心业务实体。

```text
ReleasePlan
- id
- project_id
- name
- version
- description
- release_owner_id
- planned_time
- status
- created_by
- created_at
- updated_at
```

状态：

```text
DRAFT
READY
TEST_MERGING
WAIT_CONFLICT_RESOLVE
TEST_DEPLOYING
TEST_DEPLOY_SUCCESS
WAIT_TEST_ACCEPT
TEST_REJECTED
TEST_ACCEPTED
RELEASE_BRANCH_CREATING
RELEASE_BRANCH_CREATED
PRE_DEPLOYING
PRE_DEPLOY_SUCCESS
PROD_DEPLOYING
PROD_DEPLOY_SUCCESS
WAIT_PROD_CONFIRM
COMPLETED
FAILED
CANCELLED
```

---

# 9. 发布计划关联人员

```text
ReleasePlanMember
- release_plan_id
- user_id
- role
```

角色：

```text
DEVELOPER
TESTER
PRODUCT
RELEASE_OWNER
```

---

# 10. 发布计划关联代码

一个发布计划支持多个服务：

```text
ReleasePlanService
- id
- release_plan_id
- service_id
- repository_id
- source_branch
- target_test_branch
- release_branch
- commit_id
```

例如：

```text
发布计划 10086

order-service
feature/order-123
→ release_test

order-web
feature/web-456
→ release_test
```

---

# 11. 环境管理

环境不能硬编码。

管理员可以配置：

```text
Environment
- id
- project_id
- code
- name
- type
- sequence
- enabled
- config
```

例如：

```text
DEV
TEST
SIT
UAT
PRE
PROD
GRAY
```

环境类型：

```text
DEVELOPMENT
TEST
PRE_RELEASE
PRODUCTION
OTHER
```

---

# 12. 环境发布规则

管理员可以配置：

```text
EnvironmentReleaseRule
- environment_id
- need_test_accept
- need_release_branch
- need_confirm
- allow_auto_merge
- allow_retry
- timeout_minutes
- enabled
```

例如：

### TEST

```text
自动 Merge：是
需要测试验收：否
需要 Release Branch：否
```

### PRE

```text
自动 Merge：否
需要测试验收：是
需要 Release Branch：是
```

### PROD

```text
需要测试验收：是
需要 Release Branch：是
发布完成需要测试确认：是
```

---

# 13. 发布流程配置

管理员可以配置发布流程。

```text
ReleaseFlow
- id
- project_id
- name
- enabled
- version
```

流程步骤：

```text
ReleaseFlowStep
- id
- flow_id
- step_code
- step_name
- step_type
- sequence
- required
- executor_role
- timeout_minutes
- allow_retry
- allow_skip
```

步骤类型：

```text
TEST_MERGE
JENKINS_BUILD
DEPLOY
TEST_ACCEPT
CREATE_RELEASE_BRANCH
PRE_RELEASE
PROD_RELEASE
PROD_CONFIRM
NOTIFICATION
CUSTOM
```

---

# 14. 测试环境发布

点击“测试发布”后：

```text
1. 检查发布计划
2. 检查关联需求
3. 检查代码仓库
4. 检查开发分支
5. 检查目标分支
6. 执行 Merge
7. 判断是否冲突
8. 无冲突 → 进入 Jenkins
9. 有冲突 → WAIT_CONFLICT_RESOLVE
10. Jenkins Build
11. Jenkins Deploy
12. Deployment Verification
13. Health Check
14. Version Check
15. 全部成功
16. TEST_DEPLOY_SUCCESS
17. 通知发布人
```

---

# 15. Git Merge 冲突

Merge 必须由发布平台执行，不由 Jenkins 负责。

```text
开发分支
   │
   ▼
merge release_test
   │
   ├── SUCCESS
   │     ↓
   │   Jenkins
   │
   └── CONFLICT
         ↓
WAIT_CONFLICT_RESOLVE
```

冲突页面展示：

```text
仓库
源分支
目标分支
冲突文件
Commit
错误信息
```

开发人员在本地解决：

```text
git pull
解决冲突
git commit
git push
```

平台再次执行检查。

第一期不要实现在线 Git 冲突编辑器。

---

# 16. 测试验收

测试发布成功：

```text
TEST_DEPLOY_SUCCESS
        ↓
WAIT_TEST_ACCEPT
```

测试人员可以：

```text
通过
不通过
```

通过：

```text
TEST_ACCEPTED
```

不通过：

```text
TEST_REJECTED
```

只有：

```text
TEST_ACCEPTED
```

才允许创建 Release Branch。

---

# 17. Release Branch

管理员配置：

```text
release_branch_template:
release_{yyyyMMdd}_{releasePlanId}
```

例如：

```text
release_20260824_10086
```

创建规则：

```text
release_test
    ↓
release_20260824_10086
```

Release Branch 创建成功后：

```text
ReleasePlanService.release_branch
```

保存具体分支名称。

Release Branch 创建后，生产和预发必须使用该分支。

---

# 18. 预发和生产

发布计划可以选择：

```text
☑ TEST
☑ PRE
☑ PROD
```

预发和生产必须是独立的 `ReleaseTask`。

```text
ReleasePlan
 ├── TEST ReleaseTask
 ├── PRE ReleaseTask
 └── PROD ReleaseTask
```

支持：

```text
只发 PRE
只发 PROD
PRE + PROD
```

是否允许预发和生产并行由管理员配置。

默认推荐：

```text
PRE → SUCCESS
       ↓
PROD
```

如果管理员明确允许，可以：

```text
PRE ─────────┐
             ├── SUCCESS
PROD ────────┘
```

---

# 19. Jenkins 集成

Jenkins 是 CI/CD 执行引擎。

发布平台必须支持 Jenkins REST API。

需要实现：

```text
getJob()
build()
buildWithParameters()
getQueueItem()
getBuild()
getBuildConsole()
stopBuild()
```

---

# 20. Jenkins Server 配置

```text
JenkinsServer
- id
- name
- url
- credential_id
- enabled
- created_at
- updated_at
```

认证信息不能明文保存。

至少采用：

```text
加密存储
```

生产环境优先接入：

```text
Secret Manager
```

---

# 21. Jenkins Job 配置

```text
JenkinsJob
- id
- project_id
- service_id
- environment_id
- jenkins_server_id
- job_name
- enabled
```

例如：

```text
order-service
TEST → order-service-test
PRE  → order-service-pre
PROD → order-service-prod
```

---

# 22. Jenkins 参数映射

不同 Job 参数可能不同，因此必须配置参数映射。

```text
JenkinsParameterMapping
- id
- jenkins_job_id
- platform_field
- jenkins_parameter
- required
- default_value
```

例如：

```text
sourceBranch → BRANCH
environment → ENV
releasePlanId → RELEASE_PLAN_ID
version → IMAGE_TAG
releaseBranch → RELEASE_BRANCH
```

平台调用 Jenkins 时，根据映射动态组装参数。

---

# 23. Jenkins 调用流程

```text
平台
 ↓
buildWithParameters
 ↓
Queue ID
 ↓
保存 jenkins_queue_id
 ↓
等待 Queue
 ↓
获取 Build Number
 ↓
保存 jenkins_build_number
 ↓
RUNNING
 ↓
Jenkins Pipeline
 ↓
SUCCESS / FAILURE
```

发布任务保存：

```text
jenkins_server_id
jenkins_job_name
jenkins_queue_id
jenkins_build_number
jenkins_url
jenkins_status
```

---

# 24. Jenkins Pipeline 职责

Jenkins 负责：

```text
Checkout
 ↓
Build
 ↓
Unit Test
 ↓
Docker Build
 ↓
Docker Push
 ↓
Deploy
 ↓
Rollout
 ↓
Health Check
 ↓
Version Check
 ↓
SUCCESS / FAILURE
```

平台不应该直接执行：

```text
mvn
npm
docker
kubectl
```

这些操作应该由 Jenkins 执行。

---

# 25. Jenkins Webhook

优先使用 Jenkins Webhook 通知平台。

```text
Jenkins
 ↓
POST
/api/v1/jenkins/webhook/build
 ↓
发布平台
```

Webhook 数据至少包含：

```text
job
buildNumber
status
releasePlanId
releaseTaskId
serviceId
environment
```

如果无法配置 Webhook，支持轮询 Jenkins API。

推荐：

```text
Webhook 主通道
+
Polling 兜底
```

---

# 26. 部署模型

必须支持前端和后端。

统一抽象：

```text
DeploymentTarget
```

部署类型：

```text
KUBERNETES
STATIC
OSS
CDN
SERVER
CUSTOM
```

---

# 27. Kubernetes 部署

后端项目可能是：

```text
Deployment
StatefulSet
DaemonSet
Job
```

配置：

```text
DeploymentConfig
- deployment_type
- namespace
- resource_kind
- resource_name
- expected_replicas
- timeout
- health_check
- version_check
```

---

# 28. Kubernetes 多节点成功判断

这是核心逻辑。

不能：

```text
Jenkins SUCCESS
→ 发布 SUCCESS
```

必须：

```text
Jenkins SUCCESS
 ↓
K8s Rollout
 ↓
所有目标实例 Ready
 ↓
Health Check
 ↓
Version Check
 ↓
Deployment SUCCESS
```

对于 Deployment：

```text
desiredReplicas == updatedReplicas
AND
desiredReplicas == readyReplicas
AND
desiredReplicas == availableReplicas
AND
unavailableReplicas == 0
```

才允许：

```text
Deployment SUCCESS
```

---

# 29. Kubernetes Pod 状态

平台需要记录：

```text
ReleaseDeploymentNode
- id
- deployment_id
- node_name
- pod_name
- pod_ip
- version
- status
- ready
- restart_count
- error_message
- started_at
- finished_at
```

状态：

```text
PENDING
STARTING
RUNNING
READY
FAILED
TIMEOUT
```

例如：

```text
order-service

Pod 1 READY
Pod 2 READY
Pod 3 READY
Pod 4 READY

4 / 4 Ready
```

才允许部署成功。

---

# 30. Kubernetes 滚动发布

不能看到一个新 Pod Ready 就认为成功。

必须等待：

```text
desired = 4
updated = 4
ready = 4
available = 4
unavailable = 0
```

同时确保旧版本实例已经按照发布策略退出。

---

# 31. DeploymentVerifier

增加统一验证器：

```text
DeploymentVerifier
 ├── KubernetesDeploymentVerifier
 ├── KubernetesStatefulSetVerifier
 ├── KubernetesDaemonSetVerifier
 ├── KubernetesJobVerifier
 ├── FrontendVerifier
 └── CustomVerifier
```

统一接口：

```text
verify(deployment)
```

返回：

```text
SUCCESS
FAILED
TIMEOUT
RUNNING
```

---

# 32. 健康检查

健康检查必须支持管理员配置。

```text
HealthCheckConfig
- type
- url
- method
- expected_status
- expected_body
- timeout
- retry_count
- retry_interval
```

后端：

```text
GET /actuator/health
HTTP 200
status = UP
```

前端：

```text
GET /
HTTP 200
```

支持自定义 HTTP API。

---

# 33. 版本检查

强烈建议支持版本检查。

后端：

```text
GET /api/system/version
```

前端：

```text
GET /version.json
```

返回：

```json
{
  "version": "2026.08.24.10086",
  "commit": "a83fd29"
}
```

平台验证：

```text
当前运行版本 == 本次发布版本
```

不一致：

```text
VERSION_CHECK_FAILED
```

---

# 34. 前端项目发布

例如 Vue/React：

```text
Jenkins
 ↓
npm install
 ↓
npm run build
 ↓
dist
 ↓
上传 OSS / 静态服务器
 ↓
CDN Refresh
 ↓
HTTP Health Check
 ↓
Version Check
 ↓
SUCCESS
```

前端不需要 K8s Pod 判断。

最终统一为：

```text
DeploymentVerifier
```

---

# 35. 发布成功判定

最终规则：

```text
Jenkins SUCCESS
AND
Deployment SUCCESS
AND
Health Check SUCCESS
AND
Version Check SUCCESS
```

才允许：

```text
ReleaseTask SUCCESS
```

如果不需要某个检查，则根据管理员配置跳过。

---

# 36. 发布通知

通知必须在最终状态确定以后发送。

错误方式：

```text
Jenkins SUCCESS
 ↓
通知发布成功
```

正确方式：

```text
Jenkins SUCCESS
 ↓
Deployment SUCCESS
 ↓
所有实例 Ready
 ↓
Health Check SUCCESS
 ↓
Version Check SUCCESS
 ↓
ReleaseTask SUCCESS
 ↓
Environment Release SUCCESS
 ↓
Notification
```

---

# 37. 通知渠道

支持：

```text
企业微信
飞书
钉钉
邮件
站内消息
```

设计：

```text
NotificationProvider
 ├── WeComProvider
 ├── FeishuProvider
 ├── DingTalkProvider
 ├── EmailProvider
 └── InternalProvider
```

---

# 38. 通知规则

管理员可以配置：

```text
发布成功
发布失败
发布超时
生产发布完成
测试验收
报警
报警升级
```

通知对象：

```text
发布人
开发负责人
测试负责人
产品负责人
项目负责人
报警负责人
指定用户
指定群组
```

---

# 39. 生产发布确认

生产部署完成：

```text
PROD_DEPLOY_SUCCESS
 ↓
WAIT_PROD_CONFIRM
```

测试人员点击：

```text
确认发布完成
```

才：

```text
COMPLETED
```

管理员可以配置：

```text
是否需要生产确认
确认角色
确认超时时间
是否允许管理员代确认
```

---

# 40. 配置快照

每次发布开始时必须保存：

```text
ReleaseConfigSnapshot
```

保存：

```text
发布流程
环境配置
Jenkins 配置
Jenkins Job
Git 配置
部署配置
健康检查
通知规则
权限规则
```

例如：

```text
Release #10086
Config Version = 20260824.15
```

以后管理员修改配置，不影响历史发布。

---

# 41. 发布记录

必须保存完整链路：

```text
需求
 ↓
发布计划
 ↓
Git Repository
 ↓
Branch
 ↓
Commit
 ↓
Release Branch
 ↓
Jenkins Job
 ↓
Jenkins Build
 ↓
Deployment
 ↓
Pod / Target
 ↓
Health Check
 ↓
Version Check
 ↓
Notification
```

这样可以实现完整审计。

---

# 42. ReleaseTask

一个发布计划可以包含多个环境任务。

```text
ReleasePlan
 ├── TestReleaseTask
 ├── PreReleaseTask
 └── ProdReleaseTask
```

ReleaseTask：

```text
ReleaseTask
- id
- release_plan_id
- environment_id
- status
- started_at
- finished_at
- error_message
```

---

# 43. ReleaseDeployment

一个环境任务可以包含多个服务。

```text
ReleaseTask
 ├── order-service
 ├── order-web
 └── order-job
```

数据：

```text
ReleaseDeployment
- id
- release_task_id
- service_id
- deployment_type
- status
- started_at
- finished_at
```

---

# 44. 状态机

发布状态必须通过状态机管理，禁止在业务代码中大量使用 if/else 修改状态。

核心流程：

```text
DRAFT
 ↓
READY
 ↓
TEST_MERGING
 ├── CONFLICT
 │    ↓
 │ WAIT_CONFLICT_RESOLVE
 │    ↓
 │ TEST_MERGING
 │
 ↓
TEST_DEPLOYING
 ↓
TEST_DEPLOY_SUCCESS
 ↓
WAIT_TEST_ACCEPT
 ↓
TEST_ACCEPTED
 ↓
RELEASE_BRANCH_CREATING
 ↓
RELEASE_BRANCH_CREATED
 ↓
PRE_DEPLOYING / PROD_DEPLOYING
 ↓
DEPLOY_SUCCESS
 ↓
WAIT_PROD_CONFIRM
 ↓
COMPLETED
```

失败：

```text
FAILED
TIMEOUT
CANCELLED
```

---

# 45. 报警中心

外部系统通过 Webhook 接入。

接口：

```http
POST /api/v1/alerts/webhook/{projectKey}
```

统一报警模型：

```text
Alert
- id
- project_id
- source
- external_alert_id
- title
- content
- level
- status
- environment
- service
- labels
- first_occurred_at
- last_occurred_at
- acknowledged_by
- acknowledged_at
- resolved_at
```

级别：

```text
INFO
WARNING
ERROR
CRITICAL
```

---

# 46. 报警 Provider

```text
AlertProvider
 ├── Prometheus
 ├── Grafana
 ├── Cloud
 └── CustomWebhook
```

第一期重点支持：

```text
Custom Webhook
```

让其他系统可以直接调用。

---

# 47. 报警路由

报警收到以后：

```text
Alert
 ↓
Project
 ↓
Service
 ↓
Environment
 ↓
Labels
 ↓
Alert Rule
 ↓
Receiver
```

例如：

```text
order-service
production
database
CRITICAL

→ 后端负责人
→ DBA
→ 技术负责人
```

---

# 48. 报警频率

管理员可以配置：

```text
首次通知：立即
重复通知：5分钟
最大重复次数：3
```

例如：

```text
0分钟  → 通知
5分钟  → 通知
10分钟 → 通知
15分钟 → 停止普通重复通知
```

---

# 49. ACK / 处理中

报警通知：

```text
[查看报警]
[处理中]
```

点击“处理中”：

```text
ALERTING
 ↓
ACKNOWLEDGED
```

默认规则：

> ACK 后停止普通重复通知，但如果报警持续超过升级时间，仍然允许触发报警升级。

---

# 50. 报警升级

管理员配置：

```text
Level 1
0分钟
→ 开发负责人

Level 2
10分钟
→ 技术负责人

Level 3
20分钟
→ 项目负责人

Level 4
30分钟
→ 值班负责人
```

升级配置：

```text
AlertEscalation
- id
- alert_rule_id
- level
- delay_minutes
- receivers
- channels
```

---

# 51. 报警恢复

外部系统恢复以后：

```text
ALERTING
 ↓
RESOLVED
```

发送恢复通知：

```text
【报警恢复】

订单服务
生产环境

报警：接口错误率过高
开始时间：22:30
恢复时间：22:43
持续时间：13分钟
```

---

# 52. 报警去重

使用：

```text
fingerprint
```

生成规则：

```text
project
+
service
+
environment
+
alert_rule
+
labels
```

相同 fingerprint 在短时间内视为同一个报警。

避免：

```text
同一个报警
一分钟 1000 次 Webhook
→ 发送 1000 条消息
```

正确行为：

```text
1000 次事件
 ↓
1 个 Alert
 ↓
更新 last_occurred_at
 ↓
根据频率规则通知
```

---

# 53. Redis 的使用

建议 Redis 用于：

```text
报警去重
报警频率控制
分布式锁
Jenkins 状态缓存
发布任务锁
幂等控制
```

例如：

```text
alert:fingerprint:{fingerprint}
release:lock:{releaseTaskId}
jenkins:build:{job}:{buildNumber}
```

---

# 54. MQ 的使用

发布和报警都可能产生大量异步任务。

推荐：

```text
RabbitMQ / Kafka
```

事件：

```text
ReleaseStarted
JenkinsBuildStarted
JenkinsBuildFinished
DeploymentStarted
DeploymentFinished
ReleaseSucceeded
ReleaseFailed

AlertReceived
AlertNotify
AlertAcknowledged
AlertEscalated
AlertResolved
```

通知建议异步执行：

```text
业务线程
 ↓
MQ
 ↓
Notification Worker
 ↓
企业微信 / 飞书
```

避免消息平台异常影响发布主流程。

---

# 55. 幂等要求

所有外部回调必须幂等。

例如 Jenkins Webhook 重复发送：

```text
Build #582 SUCCESS
Build #582 SUCCESS
Build #582 SUCCESS
```

系统只能处理一次。

唯一键：

```text
jenkins_server_id
+
job_name
+
build_number
```

报警也使用：

```text
project
+
fingerprint
+
external_event_id
```

---

# 56. 权限

采用 RBAC。

角色：

```text
SUPER_ADMIN
SYSTEM_ADMIN
PROJECT_ADMIN
RELEASE_OWNER
DEVELOPER
TESTER
PRODUCT
ALERT_OWNER
VIEWER
```

生产发布权限必须单独控制。

例如：

```text
发布计划编辑
≠
生产发布
≠
生产确认
≠
系统配置
```

---

# 57. 管理员配置权限

系统管理员可以：

```text
管理项目
管理环境
管理 Jenkins
管理 Git
管理部署
管理通知
管理报警
管理发布流程
管理用户角色
```

项目管理员只能管理所属项目。

---

# 58. 操作日志

所有重要操作记录：

```text
OperationLog
- id
- user_id
- module
- action
- target_type
- target_id
- request_id
- before_data
- after_data
- ip
- created_at
```

至少记录：

```text
创建发布计划
修改发布计划
测试发布
Git Merge
解决冲突
测试验收
创建 Release Branch
预发发布
生产发布
生产确认
修改 Jenkins 配置
修改环境配置
修改报警规则
ACK 报警
报警升级
```

---

# 59. API 设计

API 使用 REST 风格。

## 发布计划

```http
POST   /api/release-plans
GET    /api/release-plans
GET    /api/release-plans/{id}
PUT    /api/release-plans/{id}
DELETE /api/release-plans/{id}
```

## 发布

```http
POST /api/release-plans/{id}/test-release
POST /api/release-plans/{id}/create-release-branch
POST /api/release-plans/{id}/pre-release
POST /api/release-plans/{id}/prod-release
POST /api/release-tasks/{id}/cancel
POST /api/release-tasks/{id}/retry
```

## 验收

```http
POST /api/release-plans/{id}/test-accept
POST /api/release-plans/{id}/test-reject
POST /api/release-plans/{id}/prod-confirm
```

## Jenkins

```http
POST /api/jenkins/webhook/build
GET  /api/jenkins/jobs/{id}
GET  /api/jenkins/builds/{id}
GET  /api/jenkins/builds/{id}/console
```

## 报警

```http
POST /api/v1/alerts/webhook/{projectKey}
GET  /api/alerts
GET  /api/alerts/{id}
POST /api/alerts/{id}/ack
POST /api/alerts/{id}/resolve
```

---

# 60. 配置 API

```http
GET  /api/admin/projects
POST /api/admin/projects

GET  /api/admin/environments
POST /api/admin/environments

GET  /api/admin/jenkins/servers
POST /api/admin/jenkins/servers

GET  /api/admin/jenkins/jobs
POST /api/admin/jenkins/jobs

GET  /api/admin/deployment-configs
POST /api/admin/deployment-configs

GET  /api/admin/notification-channels
POST /api/admin/notification-channels

GET  /api/admin/alert-rules
POST /api/admin/alert-rules

GET  /api/admin/release-flows
POST /api/admin/release-flows
```

---

# 61. 前端页面

## Dashboard

显示：

```text
今日发布
测试发布
预发发布
生产发布
成功率
失败发布
当前报警
Critical 报警
待验收发布
待生产确认
```

## 发布计划列表

支持：

```text
项目
版本
状态
负责人
发布时间
环境
创建时间
```

## 发布详情

建议采用时间线：

```text
需求
 ↓
代码
 ↓
测试 Merge
 ↓
测试发布
 ↓
测试验收
 ↓
Release Branch
 ↓
预发
 ↓
生产
 ↓
确认
 ↓
完成
```

---

# 62. 发布详情中的部署节点

例如：

```text
order-service

Jenkins #582
SUCCESS

Kubernetes
Deployment: order-service
Namespace: order

4 / 4 Ready

Pod                         状态
order-service-xxx-1         Ready
order-service-xxx-2         Ready
order-service-xxx-3         Ready
order-service-xxx-4         Ready

Health Check                ✓
Version Check               ✓
```

---

# 63. 管理员配置页面

至少包含：

```text
项目
环境
发布流程
Git
Jenkins
Jenkins Job
部署方式
K8s 配置
健康检查
通知渠道
通知规则
报警规则
报警升级
用户
角色
权限
系统参数
```

---

# 64. 系统配置原则

所有配置必须遵循：

```text
数据库配置
 ↓
配置缓存
 ↓
发布时读取
 ↓
创建 Config Snapshot
 ↓
按照 Snapshot 执行
```

不要：

```text
Java 常量
if environment == "prod"
if project == "order"
if job == "xxx"
```

禁止大量硬编码。

---

# 65. 关键技术原则

## 65.1 发布平台不是 Jenkins

Jenkins：

```text
执行
```

平台：

```text
编排
```

## 65.2 发布平台不是 K8s

K8s：

```text
部署
```

平台：

```text
判断部署是否达到成功条件
```

## 65.3 Pod 不是唯一部署对象

统一：

```text
DeploymentTarget
```

支持：

```text
K8s
OSS
CDN
Static
Custom
```

## 65.4 所有成功都必须有明确条件

禁止：

```text
HTTP 200 = 发布成功
```

必须根据配置：

```text
Build
+
Deployment
+
Ready
+
Health
+
Version
```

---

# 66. 发布成功的最终判定

统一使用：

```text
ReleaseSuccessEvaluator
```

伪代码：

```java
boolean success(ReleaseTask task) {

    if (!task.jenkinsSuccess()) {
        return false;
    }

    if (!task.deploymentSuccess()) {
        return false;
    }

    if (config.needHealthCheck()
            && !task.healthCheckSuccess()) {
        return false;
    }

    if (config.needVersionCheck()
            && !task.versionCheckSuccess()) {
        return false;
    }

    return true;
}
```

Kubernetes：

```java
boolean k8sSuccess(Deployment deployment) {

    return deployment.desiredReplicas == deployment.updatedReplicas
        && deployment.desiredReplicas == deployment.readyReplicas
        && deployment.desiredReplicas == deployment.availableReplicas
        && deployment.unavailableReplicas == 0;
}
```

---

# 67. 发布通知最终触发条件

```text
ReleaseTask
   ↓
Jenkins SUCCESS
   ↓
Deployment SUCCESS
   ↓
All Target SUCCESS
   ↓
Health Check SUCCESS
   ↓
Version Check SUCCESS
   ↓
ReleaseSuccessEvaluator
   ↓
ReleaseTask = SUCCESS
   ↓
Environment Release = SUCCESS
   ↓
Notification Event
   ↓
企业微信 / 飞书
```

**只有最后一步之前的全部条件满足，才能发送“部署成功”消息。**

---

# 68. 第一阶段 MVP

优先实现：

### 发布

```text
[ ] 项目
[ ] 项目成员
[ ] 手动需求
[ ] 云效需求
[ ] Git 仓库
[ ] Git 分支
[ ] 发布计划
[ ] 测试环境
[ ] release_test Merge
[ ] Git 冲突
[ ] Jenkins
[ ] 测试发布
[ ] 测试验收
[ ] Release Branch
[ ] 预发
[ ] 生产
[ ] 生产确认
[ ] 发布记录
[ ] 操作日志
```

### 部署

```text
[ ] Jenkins REST API
[ ] Jenkins Queue
[ ] Jenkins Build
[ ] Jenkins Webhook
[ ] K8s Deployment
[ ] 多 Pod Ready
[ ] Health Check
[ ] Version Check
[ ] 前端 OSS/CDN
[ ] 发布成功判定
```

### 通知

```text
[ ] 企业微信
[ ] 飞书
[ ] 发布成功
[ ] 发布失败
[ ] 发布超时
```

### 报警

```text
[ ] Webhook
[ ] 报警规则
[ ] 报警负责人
[ ] 报警去重
[ ] 报警频率
[ ] ACK
[ ] 报警升级
[ ] 报警恢复
```

### 管理员

```text
[ ] 项目配置
[ ] 环境配置
[ ] 发布流程配置
[ ] Jenkins 配置
[ ] Git 配置
[ ] 部署配置
[ ] 健康检查配置
[ ] 通知配置
[ ] 报警配置
[ ] 用户
[ ] 角色
[ ] 权限
[ ] 配置版本
```

---

# 69. AI Coding 实现要求

AI Coding 在实现本项目时必须遵循以下规则：

1. **先建立领域模型，再实现 API。**
2. **先实现状态机，再实现发布流程。**
3. 外部系统全部采用 Provider / Adapter 设计。
4. Jenkins、Git、云效、通知渠道、K8s 均不得与核心业务强耦合。
5. 所有异步任务必须具备幂等能力。
6. Jenkins Webhook 必须支持重复回调。
7. 报警 Webhook 必须支持重复事件。
8. 发布任务必须支持失败重试。
9. 发布任务必须支持取消。
10. 生产发布必须进行权限校验。
11. 所有生产操作必须记录操作日志。
12. 每次发布必须生成配置快照。
13. 不允许通过 Jenkins `SUCCESS` 单独判断发布成功。
14. K8s 必须等待所有目标实例达到 Ready/Available。
15. 前端项目必须支持非 K8s 发布方式。
16. 发布成功通知必须在最终成功状态确定以后发送。
17. 通知发送失败不能修改已经完成的发布状态。
18. 报警 ACK 后默认停止普通重复通知，但仍允许触发升级。
19. 所有超时操作必须有明确状态。
20. 所有外部调用必须配置 timeout、retry 和错误处理。
21. 所有敏感凭证必须加密存储。
22. 前端不得获取 Jenkins Token、Git Token 等敏感信息。
23. 不允许把项目名称、Jenkins Job、环境名称、分支名称写死在代码中。
24. 数据库迁移必须使用 Flyway/Liquibase 等版本化工具。
25. 所有核心业务必须有单元测试和集成测试。

---

# 70. 推荐代码分层

如果采用 Spring Boot：

```text
com.xxx.release
│
├── common
│   ├── exception
│   ├── response
│   ├── security
│   ├── utils
│   └── enums
│
├── project
│   ├── controller
│   ├── service
│   ├── repository
│   └── domain
│
├── release
│   ├── controller
│   ├── service
│   ├── state
│   ├── domain
│   ├── repository
│   └── event
│
├── git
│   ├── GitProvider
│   ├── GitLabProvider
│   ├── CodeupProvider
│   └── service
│
├── jenkins
│   ├── JenkinsProvider
│   ├── JenkinsClient
│   ├── webhook
│   └── service
│
├── deployment
│   ├── DeploymentAdapter
│   ├── KubernetesAdapter
│   ├── FrontendAdapter
│   ├── verifier
│   └── health
│
├── notification
│   ├── NotificationProvider
│   ├── WeComProvider
│   ├── FeishuProvider
│   └── service
│
├── alert
│   ├── controller
│   ├── service
│   ├── routing
│   ├── escalation
│   ├── fingerprint
│   └── notification
│
├── config
│   ├── project
│   ├── environment
│   ├── release-flow
│   ├── jenkins
│   ├── deployment
│   └── notification
│
└── audit
    ├── service
    └── repository
```

---

# 71. 推荐开发顺序

AI Coding 不要一次生成整个系统，按照以下顺序迭代：

```text
Phase 1
基础工程
 ↓
用户 / RBAC
 ↓
项目管理
 ↓
环境管理
```

```text
Phase 2
Git Provider
 ↓
需求管理
 ↓
发布计划
 ↓
发布状态机
```

```text
Phase 3
Jenkins Server
 ↓
Jenkins Job
 ↓
Jenkins API
 ↓
Queue / Build
 ↓
Webhook
```

```text
Phase 4
测试发布
 ↓
Git Merge
 ↓
冲突处理
 ↓
Jenkins Test
```

```text
Phase 5
测试验收
 ↓
Release Branch
 ↓
预发
 ↓
生产
 ↓
生产确认
```

```text
Phase 6
Deployment Adapter
 ↓
Kubernetes
 ↓
多 Pod 验证
 ↓
Health Check
 ↓
Version Check
```

```text
Phase 7
前端部署
 ↓
OSS / CDN
 ↓
HTTP / Version Check
```

```text
Phase 8
Notification
 ↓
企业微信
 ↓
飞书
```

```text
Phase 9
Alert
 ↓
Webhook
 ↓
路由
 ↓
去重
 ↓
频率
 ↓
ACK
 ↓
升级
 ↓
恢复
```

```text
Phase 10
管理员配置中心
 ↓
配置版本
 ↓
发布配置快照
 ↓
完整审计
```

---

# 72. 最终验收标准

## 发布管理

- 可以创建发布计划。
- 可以关联手动需求。
- 可以从云效导入需求。
- 可以关联多个代码仓库。
- 可以关联多个开发分支。
- 可以管理开发、测试、产品、发布人员。
- 可以发布测试环境。
- 测试发布前自动 Merge 到 `release_test`。
- Git 冲突时发布必须暂停。
- 测试人员可以验收。
- 测试验收通过后才能创建 Release Branch。
- Release Branch 格式支持 `release_年月日_id`。
- 可以独立发布预发和生产。
- 可以配置预发和生产是否并行。
- 生产发布完成后支持测试人员确认。
- 每个环境拥有独立发布任务。
- 每个服务拥有独立部署记录。

## Jenkins

- 支持 Jenkins Server 配置。
- 支持 Jenkins Job 配置。
- 支持参数映射。
- 支持 Queue。
- 支持 Build。
- 支持 Build Console。
- 支持 Webhook。
- 支持轮询兜底。
- 支持失败重试。
- 支持取消。

## K8s

- 支持 Deployment。
- 支持多 Pod。
- 必须等待所有实例 Ready。
- 必须支持 Rollout 超时。
- 必须支持 Health Check。
- 必须支持 Version Check。
- 任意实例失败都不能判定整个部署成功。
- 所有实例成功后才能发送部署成功通知。

## 前端

- 支持 Vue/React 等静态项目。
- 支持 OSS/CDN。
- 支持静态服务器。
- 支持 HTTP Health Check。
- 支持 Version Check。
- 不依赖 Kubernetes Pod 判断。

## 报警

- 支持 Webhook。
- 支持项目级路由。
- 支持负责人配置。
- 支持频率配置。
- 支持重复报警抑制。
- 支持 ACK。
- ACK 后停止普通重复通知。
- 支持报警升级。
- 支持恢复通知。
- 支持报警历史。
- 支持报警审计。

## 管理员

- 项目可配置。
- 环境可配置。
- 发布流程可配置。
- Git 可配置。
- Jenkins 可配置。
- 部署方式可配置。
- K8s 成功条件可配置。
- Health Check 可配置。
- Version Check 可配置。
- 通知可配置。
- 报警可配置。
- 报警升级可配置。
- 权限可配置。
- 配置有版本。
- 发布保存配置快照。

---

# 73. 最核心的业务闭环

最终系统必须保证下面这条链路完整可追溯：

```text
需求
 ↓
发布计划
 ↓
Git 分支
 ↓
Merge release_test
 ↓
Jenkins
 ↓
测试环境
 ↓
K8s/前端部署验证
 ↓
测试验收
 ↓
Release Branch
 ↓
Jenkins
 ↓
预发
 ↓
Jenkins
 ↓
生产
 ↓
所有部署目标成功
 ↓
Health Check
 ↓
Version Check
 ↓
生产确认
 ↓
发布完成
 ↓
企业微信 / 飞书通知
```

报警闭环：

```text
外部系统
 ↓
Webhook
 ↓
报警去重
 ↓
报警规则
 ↓
负责人路由
 ↓
企业微信 / 飞书
 ↓
处理中 ACK
 ↓
停止普通重复报警
 ↓
持续未恢复
 ↓
报警升级
 ↓
恢复
 ↓
恢复通知
 ↓
报警关闭
```

最终目标是建设一个**配置驱动、状态机驱动、Jenkins 执行、部署验证、消息闭环、完整审计**的企业级发布与报警平台，而不是简单的 Jenkins Web 管理页面。