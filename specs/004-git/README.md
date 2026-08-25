# 004-git

状态：⬜ 待 Phase 6 填充
范围：GitProvider 抽象（GitLab/GitHub/Gitee/Codeup/Custom）统一接口 getRepository/getBranches/createBranch/mergeBranch/checkMerge/getDiff、Repository 与凭证管理（加密）、merge 冲突检测与 WAIT_CONFLICT_RESOLVE 流程（规范 §7/§15）。FakeGitServer 测试。
