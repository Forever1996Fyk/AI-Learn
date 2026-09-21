package com.forever1996Fyk.ai.agentplus.sys.service;

import com.forever1996Fyk.ai.agentplus.sys.entity.SysRole;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务（只读列表，不做角色 CRUD）。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SysRoleMapper sysRoleMapper;

    /**
     * 查询所有启用状态的角色，按 sort 排序。
     */
    public List<SysRole> list() {
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, STATUS_ACTIVE)
                        .orderByAsc(SysRole::getSort));
    }
}
