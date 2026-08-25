package com.company.release.requirement.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.project.repository.ProjectRepository;
import com.company.release.requirement.domain.RequirementEntity;
import com.company.release.requirement.provider.RequirementProvider;
import com.company.release.requirement.repository.RequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 需求管理（规范 §6）：手动创建 + Provider 导入（按业务唯一键幂等）。
 */
@Service
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;
    private final Map<String, RequirementProvider> providersByType;

    public RequirementService(RequirementRepository requirementRepository,
                              ProjectRepository projectRepository,
                              Map<String, RequirementProvider> providersByType) {
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
        this.providersByType = providersByType;
    }

    @Transactional
    public RequirementEntity createManual(Long projectId, String title,
                                          String description, String priority) {
        requireProjectExists(projectId);
        var r = new RequirementEntity();
        r.setProjectId(projectId);
        r.setTitle(title);
        r.setDescription(description);
        r.setPriority(priority);
        r.setSourceType("MANUAL");
        return requirementRepository.save(r);
    }

    /** 从外部系统导入；重复导入返回已有记录（ADR-010 业务唯一键幂等）。 */
    @Transactional
    public RequirementEntity importFromProvider(Long projectId, String sourceType, String externalId) {
        requireProjectExists(projectId);
        RequirementProvider provider = providersByType.get(sourceType);
        if (provider == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "no provider registered for source type: " + sourceType);
        }
        Optional<RequirementEntity> existing =
                requirementRepository.findByProjectIdAndSourceTypeAndExternalId(projectId, sourceType, externalId);
        if (existing.isPresent()) {
            return existing.get();
        }
        var detail = provider.getDetail(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "external requirement not found: " + externalId));
        var r = new RequirementEntity();
        r.setProjectId(projectId);
        r.setTitle(detail.title());
        r.setSourceType(sourceType);
        r.setExternalId(detail.externalId());
        r.setExternalUrl(detail.url());
        return requirementRepository.save(r);
    }

    @Transactional(readOnly = true)
    public List<RequirementEntity> listByProject(Long projectId) {
        return requirementRepository.findByProjectId(projectId);
    }

    /** 供前端搜索外部需求列表。 */
    @Transactional(readOnly = true)
    public List<RequirementProvider.ExternalRequirement> search(String sourceType, String keyword) {
        RequirementProvider provider = providersByType.get(sourceType);
        if (provider == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "no provider registered for source type: " + sourceType);
        }
        return provider.search(keyword);
    }

    private void requireProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "project not found: " + projectId);
        }
    }
}
