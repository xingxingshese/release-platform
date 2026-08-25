package com.company.release.requirement.provider;

import java.util.List;
import java.util.Optional;

/**
 * 需求源 Provider 抽象（规范 §6.2）：Yunxiao/Jira/Tapd/Custom 均实现此接口。
 * 实现不得与发布核心业务耦合；测试使用 Fake。
 */
public interface RequirementProvider {

    /** 对应 source_type：YUNXIAO / JIRA / TAPD / OTHER。 */
    String sourceType();

    record ExternalRequirement(String externalId, String title, String url) {
    }

    List<ExternalRequirement> search(String keyword);

    Optional<ExternalRequirement> getDetail(String externalId);
}
