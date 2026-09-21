package com.forever1996Fyk.ai.agentplus.sys.service;

import com.forever1996Fyk.ai.agentplus.sys.entity.SysDept;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门内存树（用于 data-agent 数据权限计算）。
 * <p>
 * 启动时把 sys_dept 全量加载到邻接表（parentId → childIdList），
 * 提供 collectWithSubtree / collectSelfOnly 给 DataScopeResolver 做 BFS。
 * 数据量小（典型 <100 节点），内存查询比 Redis 一次网络往返快。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptTreeCache {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SysDeptMapper sysDeptMapper;

    /**
     * 邻接表：parentId → 子部门 ID 列表
     */
    private volatile Map<Long, List<Long>> childrenMap = Collections.emptyMap();

    /**
     * 所有部门 ID 集合（用于快速判断 deptId 是否存在）
     */
    private volatile Set<Long> allDeptIds = Collections.emptySet();

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重新加载 sys_dept 到内存树。
     * 启动失败不抛出（让应用启动，admin 可登录修数据），缓存为空时 resolve() 会 fail-closed。
     */
    public synchronized void reload() {
        try {
            List<SysDept> all = sysDeptMapper.selectList(
                    new LambdaQueryWrapper<SysDept>().eq(SysDept::getStatus, STATUS_ACTIVE));
            Map<Long, List<Long>> map = new HashMap<>();
            Set<Long> ids = new HashSet<>();
            for (SysDept d : all) {
                ids.add(d.getId());
                map.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(d.getId());
            }
            this.childrenMap = map;
            this.allDeptIds = ids;
            log.info("[DeptTreeCache] 加载完成：{} 个部门", all.size());
        } catch (Exception e) {
            log.error("[DeptTreeCache] 加载失败，缓存为空。data-agent 数据权限将拒绝执行", e);
            this.childrenMap = Collections.emptyMap();
            this.allDeptIds = Collections.emptySet();
        }
    }

    /**
     * 是否已加载（启动加载成功则 true）。
     */
    public boolean isLoaded() {
        return !allDeptIds.isEmpty();
    }

    /**
     * 收集 deptId 自身 + 所有子孙部门（BFS 遍历邻接表）。
     * 用于 DEPT_AND_SUB scope。
     */
    public List<Long> collectWithSubtree(Long deptId) {
        if (deptId == null || !allDeptIds.contains(deptId)) {
            return List.of();
        }
        Set<Long> visited = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            if (visited.add(cur)) {
                List<Long> children = childrenMap.get(cur);
                if (children != null) {
                    queue.addAll(children);
                }
            }
        }
        return new ArrayList<>(visited);
    }

    /**
     * 仅收集 deptId 自身（不含子孙）。
     * 用于 DEPT scope。
     */
    public List<Long> collectSelfOnly(Long deptId) {
        if (deptId == null || !allDeptIds.contains(deptId)) {
            return List.of();
        }
        return List.of(deptId);
    }
}
