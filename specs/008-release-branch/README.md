# 008-release-branch

状态：⬜ 待 Phase 10 填充
范围：release_branch_template 配置化（默认 release_{yyyyMMdd}_{releasePlanId}），基于 release_test 创建，写入 ReleasePlanService.release_branch；重复创建幂等（规范 §17）。
