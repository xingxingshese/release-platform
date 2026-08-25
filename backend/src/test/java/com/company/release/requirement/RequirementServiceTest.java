package com.company.release.requirement;

import com.company.release.common.exception.BusinessException;
import com.company.release.requirement.application.RequirementService;
import com.company.release.requirement.domain.RequirementEntity;
import com.company.release.requirement.provider.RequirementProvider;
import com.company.release.requirement.repository.RequirementRepository;
import com.company.release.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 规范 §6：三种需求来源（手动/云效/Jira/Tapd），Provider 抽象，导入幂等。
 */
class RequirementServiceTest {

    private RequirementRepository requirementRepository;
    private ProjectRepository projectRepository;
    private Map<String, RequirementProvider> providers;
    private RequirementService service;

    /** 测试用 Fake Provider（agent.md §二十：外部系统一律 Fake）。 */
    static class FakeYunxiaoProvider implements RequirementProvider {
        @Override
        public String sourceType() {
            return "YUNXIAO";
        }

        @Override
        public List<ExternalRequirement> search(String keyword) {
            return List.of(
                    new ExternalRequirement("YX-100", "需求A", "https://yunxiao/req/100"),
                    new ExternalRequirement("YX-101", "需求B", "https://yunxiao/req/101"));
        }

        @Override
        public Optional<ExternalRequirement> getDetail(String externalId) {
            return search(null).stream()
                    .filter(r -> r.externalId().equals(externalId))
                    .findFirst();
        }
    }

    @BeforeEach
    void setUp() {
        requirementRepository = mock(RequirementRepository.class);
        projectRepository = mock(ProjectRepository.class);
        providers = Map.of("YUNXIAO", new FakeYunxiaoProvider());
        service = new RequirementService(requirementRepository, projectRepository, providers);
    }

    @Test
    void createManualRequirementRequiresKnownProject() {
        when(projectRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.createManual(99L, "标题", null, null))
                .hasMessageContaining("project not found");
    }

    @Test
    void manualRequirementHasSourceTypeManual() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var r = service.createManual(1L, "手工需求", "desc", "P1");
        assertThat(r.getSourceType()).isEqualTo("MANUAL");
    }

    @Test
    void importIsIdempotentOnExternalKey() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        // 已存在 (project, sourceType, externalId) → 不再保存，返回已存在记录
        when(requirementRepository.findByProjectIdAndSourceTypeAndExternalId(1L, "YUNXIAO", "YX-100"))
                .thenReturn(Optional.of(existing()));
        var result = service.importFromProvider(1L, "YUNXIAO", "YX-100");
        assertThat(result.getId()).isEqualTo(42L);
        verify(requirementRepository, never()).save(any());
    }

    @Test
    void importNewRequirementSavesWithExternalInfo() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(requirementRepository.findByProjectIdAndSourceTypeAndExternalId(eq(1L), eq("YUNXIAO"), eq("YX-101")))
                .thenReturn(Optional.empty());
        when(requirementRepository.save(any())).thenAnswer(inv -> {
            var e = (RequirementEntity) inv.getArgument(0);
            e.setId(7L);
            return e;
        });
        var result = service.importFromProvider(1L, "YUNXIAO", "YX-101");
        assertThat(result.getExternalId()).isEqualTo("YX-101");
        assertThat(result.getTitle()).isEqualTo("需求B");
        assertThat(result.getSourceType()).isEqualTo("YUNXIAO");
    }

    @Test
    void unknownSourceTypeRejected() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.importFromProvider(1L, "NOT_EXIST", "X"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("provider");
    }

    private RequirementEntity existing() {
        var e = new RequirementEntity();
        e.setId(42L);
        e.setProjectId(1L);
        e.setTitle("已有需求");
        e.setSourceType("YUNXIAO");
        e.setExternalId("YX-100");
        return e;
    }

    private static <T> T eq(T v) {
        return org.mockito.ArgumentMatchers.eq(v);
    }
}
