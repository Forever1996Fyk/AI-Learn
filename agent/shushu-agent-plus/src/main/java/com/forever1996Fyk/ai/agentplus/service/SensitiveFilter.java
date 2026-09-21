package com.forever1996Fyk.ai.agentplus.service;

import com.forever1996Fyk.ai.agentplus.domain.dto.SqlResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/21 16:10
 **/
@Slf4j
@Component
public class SensitiveFilter {
    private static final String MASK = "********";

    private final boolean enabled;
    private final Set<String> sensitiveColumns;
    private final Set<String> sensitiveFields;

    public SensitiveFilter(
            @Value("${data-agent.sensitive-filter.enabled:false}") boolean enabled,
            @Value("${data-agent.sensitive-filter.mask-fields:sys_user.password,user_profile.id_card}")
            List<String> fields) {
        this.enabled = enabled;
        List<String> normalizedFields = (fields == null) ? List.of()
                : fields.stream()
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .map(String::toLowerCase)
                  .toList();
        this.sensitiveFields = normalizedFields.stream()
                .filter(s -> s.contains("."))
                .collect(Collectors.toUnmodifiableSet());
        this.sensitiveColumns = normalizedFields.stream()
                .map(s -> {
                    int dot = s.lastIndexOf('.');
                    return (dot >= 0) ? s.substring(dot + 1) : s;
                })
                .collect(Collectors.toUnmodifiableSet());
        log.info("[SensitiveFilter] enabled={}, maskFields={}, maskColumns={}", enabled, sensitiveFields, sensitiveColumns);
    }

    public Object maskValue(String tableName, String columnName, Object value) {
        return value != null && isSensitiveField(tableName, columnName) ? MASK : value;
    }

    /**
     * 原地脱敏：优先按 JDBC 元数据识别真实字段，兜底按返回列名匹配。
     */
    public void mask(List<SqlResult.ColumnMeta> columnMetas, List<Map<String, Object>> rows) {
        if (!enabled || columnMetas == null || rows == null || rows.isEmpty()) {
            return;
        }
        List<String> hitCols = columnMetas.stream()
                .filter(this::isSensitiveColumn)
                .map(SqlResult.ColumnMeta::label)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();
        if (hitCols.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            for (String c : hitCols) {
                if (row.get(c) != null) {
                    row.put(c, MASK);
                }
            }
        }
    }

    private boolean isSensitiveColumn(SqlResult.ColumnMeta meta) {
        if (meta == null) {
            return false;
        }
        if (isSensitiveField(meta.tableName(), meta.columnName())) {
            return true;
        }
        return meta.label() != null && sensitiveColumns.contains(meta.label().toLowerCase());
    }

    public boolean isSensitiveField(String tableName, String columnName) {
        if (!enabled || columnName == null || columnName.isBlank()) {
            return false;
        }
        String col = columnName.toLowerCase();
        if (tableName != null && !tableName.isBlank()
                && sensitiveFields.contains(tableName.toLowerCase() + "." + col)) {
            return true;
        }
        return sensitiveColumns.contains(col);
    }
}
