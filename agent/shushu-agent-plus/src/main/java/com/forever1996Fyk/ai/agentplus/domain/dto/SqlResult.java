package com.forever1996Fyk.ai.agentplus.domain.dto;

import java.util.List;
import java.util.Map;

/**
 * SQL 执行结果模型。error 为 null 表示成功，非 null 表示失败。
 */
public record SqlResult(String executedSql,
                        List<String> columns,
                        List<ColumnMeta> columnMetas,
                        List<Map<String, Object>> rows,
                        int rowCount,
                        boolean truncated,
                        long durationMs,
                        String error) {

    public record ColumnMeta(String label, String tableName, String columnName) {
    }

    public boolean success() {
        return error == null;
    }

    public static SqlResult ok(String executedSql, List<String> columns,
                               List<ColumnMeta> columnMetas,
                               List<Map<String, Object>> rows, int rowCount,
                               boolean truncated, long durationMs) {
        return new SqlResult(executedSql, columns, columnMetas, rows, rowCount, truncated, durationMs, null);
    }

    public static SqlResult fail(String executedSql, String error, long durationMs) {
        return new SqlResult(executedSql, List.of(), List.of(), List.of(), 0, false, durationMs, error);
    }
}
