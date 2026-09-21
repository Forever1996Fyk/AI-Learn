package com.forever1996Fyk.ai.agentplus.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.hollis.llm.mentor.agentx.sys.entity.SysRole;
import cn.hollis.llm.mentor.agentx.sys.entity.SysUserRole;
import cn.hollis.llm.mentor.agentx.sys.mapper.SysRoleMapper;
import cn.hollis.llm.mentor.agentx.sys.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色/权限查询实现。
 *
 * 权限模型只用到角色（@SaCheckRole / StpUtil.checkRole），不做细粒度功能权限。
 * getPermissionList 是接口强制要求的方法，留空占位即可。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 不做细粒度功能权限，留空占位
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        Long userId;
        try {
            userId = Long.valueOf(loginId.toString());
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }

        // 1. 查用户的所有 roleId
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查角色信息（仅 ACTIVE 状态）
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, STATUS_ACTIVE));
        return roles.stream().map(SysRole::getCode).toList();
    }
}
