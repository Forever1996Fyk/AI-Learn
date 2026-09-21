package com.forever1996Fyk.ai.agentplus.sys.service;

import com.forever1996Fyk.ai.agentplus.sys.entity.SysDept;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门服务（只读树，不做部门 CRUD）。
 * <p>
 * 树形构建：查所有 ACTIVE 部门，按 parent_id 分组，递归填充 children 字段。
 * 返回顶级部门（parent_id = 0）的列表。
 */
@Service
@RequiredArgsConstructor
public class DeptService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final long ROOT_PARENT_ID = 0L;

    private final SysDeptMapper sysDeptMapper;

    /**
     * 构建完整的部门树。
     */
    public List<SysDept> tree() {
        List<SysDept> all = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getStatus, STATUS_ACTIVE)
                        .orderByAsc(SysDept::getSort));

        if (all.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 parent_id 分组
        Map<Long, List<SysDept>> byParent = all.stream()
                .collect(Collectors.groupingBy(SysDept::getParentId));

        // 递归填充 children
        for (SysDept dept : all) {
            dept.setChildren(byParent.getOrDefault(dept.getId(), new ArrayList<>()));
        }

        return byParent.getOrDefault(ROOT_PARENT_ID, new ArrayList<>());
    }
}
