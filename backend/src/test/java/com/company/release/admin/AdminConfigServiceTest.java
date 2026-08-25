package com.company.release.admin;

import com.company.release.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Phase 17（spec 016 / ADR-008）：版本递增 + 字段级 diff + 快照不受影响。 */
class AdminConfigServiceTest {

    private ConfigVersionRepository repository;
    private AdminConfigService service;

    @BeforeEach
    void setUp() {
        repository = mock(ConfigVersionRepository.class);
        service = new AdminConfigService(repository);
    }

    private ConfigVersionEntity version(int v, String content) {
        var e = new ConfigVersionEntity();
        e.setConfigType("environment");
        e.setConfigKey("prod");
        e.setVersion(v);
        e.setContent(content);
        return e;
    }

    @Test
    void firstSaveIsVersion1() {
        when(repository.findFirstByConfigTypeAndConfigKeyOrderByVersionDesc("environment", "prod"))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.saveVersion("environment", "prod", "{\"replicas\":2}", 9L, "init");

        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getChangedBy()).isEqualTo(9L);
    }

    @Test
    void versionsIncrementMonotonically() {
        when(repository.findFirstByConfigTypeAndConfigKeyOrderByVersionDesc("environment", "prod"))
                .thenReturn(Optional.of(version(3, "{}")));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.saveVersion("environment", "prod", "{\"replicas\":4}", 9L, "scale up");

        assertThat(saved.getVersion()).isEqualTo(4);
    }

    @Test
    void diffReturnsFieldLevelChanges() {
        when(repository.findByConfigTypeAndConfigKeyOrderByVersionDesc("environment", "prod"))
                .thenReturn(List.of(
                        version(2, "{\"replicas\":4,\"image\":\"app:v2\",\"note\":\"same\"}"),
                        version(1, "{\"replicas\":2,\"image\":\"app:v1\"}")));

        var diffs = service.diff("environment", "prod", 1, 2);

        assertThat(diffs).containsExactlyInAnyOrder(
                new AdminConfigService.DiffItem("replicas", "2", "4"),
                new AdminConfigService.DiffItem("image", "app:v1", "app:v2"),
                new AdminConfigService.DiffItem("note", null, "same"));
    }

    @Test
    void diffMissingVersionThrowsNotFound() {
        when(repository.findByConfigTypeAndConfigKeyOrderByVersionDesc("x", "y"))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.diff("x", "y", 1, 2))
                .isInstanceOf(BusinessException.class);
    }
}
