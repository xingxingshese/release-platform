package com.company.release.admin;

import com.company.release.audit.AuditLog;
import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置中心（spec 016 / ADR-008）：保存即产生新版本；diff 输出字段级差异。
 * 已生成的 ReleaseConfigSnapshot 永不回改（规范 §三十三）。
 */
@Service
public class AdminConfigService {

    private final ConfigVersionRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminConfigService(ConfigVersionRepository repository) {
        this.repository = repository;
    }

    /** 保存新版本：version = 当前最大 + 1，同 key 单调递增。 */
    @AuditLog(module = "config", action = "save-version")
    @Transactional
    public ConfigVersionEntity saveVersion(String configType, String configKey,
                                           String content, Long changedBy, String reason) {
        int next = repository.findFirstByConfigTypeAndConfigKeyOrderByVersionDesc(configType, configKey)
                .map(v -> v.getVersion() + 1).orElse(1);
        var entity = new ConfigVersionEntity();
        entity.setConfigType(configType);
        entity.setConfigKey(configKey);
        entity.setVersion(next);
        entity.setContent(content);
        entity.setChangedBy(changedBy);
        entity.setChangeReason(reason);
        return repository.save(entity);
    }

    public List<ConfigVersionEntity> versions(String configType, String configKey) {
        return repository.findByConfigTypeAndConfigKeyOrderByVersionDesc(configType, configKey);
    }

    /** 字段级对比：返回 [{path, before, after}]。 */
    public List<DiffItem> diff(String configType, String configKey, int v1, int v2) {
        var a = contentOf(configType, configKey, v1);
        var b = contentOf(configType, configKey, v2);
        return diffMaps(flatten(a), flatten(b));
    }

    private JsonNode contentOf(String type, String key, int version) {
        return repository.findByConfigTypeAndConfigKeyOrderByVersionDesc(type, key).stream()
                .filter(v -> v.getVersion() == version)
                .findFirst()
                .map(v -> {
                    try {
                        return objectMapper.readTree(v.getContent());
                    } catch (Exception e) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "bad config json: " + e.getMessage());
                    }
                })
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "config %s/%s v%d not found".formatted(type, key, version)));
    }

    static List<DiffItem> diffMaps(Map<String, String> before, Map<String, String> after) {
        var items = new ArrayList<DiffItem>();
        var keys = new java.util.LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (var k : keys) {
            String b = before.get(k);
            String a = after.get(k);
            if (!java.util.Objects.equals(b, a)) {
                items.add(new DiffItem(k, b, a));
            }
        }
        return items;
    }

    static Map<String, String> flatten(JsonNode node) {
        var out = new LinkedHashMap<String, String>();
        collect(node, "", out);
        return out;
    }

    private static void collect(JsonNode node, String prefix, Map<String, String> out) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                collect(e.getValue(), prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey(), out);
            }
        } else if (!node.isNull()) {
            out.put(prefix, node.asText());
        }
    }

    public record DiffItem(String path, String before, String after) {
    }
}
