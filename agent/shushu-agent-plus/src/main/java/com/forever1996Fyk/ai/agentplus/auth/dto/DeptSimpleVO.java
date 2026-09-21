package com.forever1996Fyk.ai.agentplus.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门简要信息（不含 parent_id/ancestors/sort/status 等内部字段）。
 */
@Data
public class DeptSimpleVO {

    private Long id;

    private String name;

    /**
     * 完整部门路径（从顶级到当前部门，含自己）。
     * 用于前端右上角区分同名部门（如多个分公司的"销售部"）。
     */
    private List<String> path = new ArrayList<>();
}
