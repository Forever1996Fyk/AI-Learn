package com.forever1996Fyk.ai.agentplus.auth.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.forever1996Fyk.ai.agentplus.auth.dto.DeptSimpleVO;
import com.forever1996Fyk.ai.agentplus.auth.dto.LoginRequest;
import com.forever1996Fyk.ai.agentplus.auth.dto.LoginUserVO;
import com.forever1996Fyk.ai.agentplus.auth.dto.RoleSimpleVO;
import com.forever1996Fyk.ai.agentplus.auth.service.AuthService;
import com.forever1996Fyk.ai.agentplus.common.enums.DataScope;
import com.forever1996Fyk.ai.agentplus.sys.dto.UserProfileVO;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysDept;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysRole;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysUser;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysUserDept;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysUserProfile;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysUserRole;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysDeptMapper;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysRoleMapper;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysUserDeptMapper;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysUserMapper;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysUserProfileMapper;
import com.forever1996Fyk.ai.agentplus.sys.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务实现。
 * <p>
 * 登录流程：查用户 → 明文校验密码 → 校验状态 → StpUtil.login → 返回 LoginUserVO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserProfileMapper sysUserProfileMapper;

    @Override
    public LoginUserVO login(LoginRequest request) {
        if (request == null
                || isBlank(request.getUsername())
                || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        // 1. 查用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("用户名不存在");
        }

        // 2. 校验密码（明文）
        if (!request.getPassword().equals(user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 3. 校验状态
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new IllegalStateException("账号已被禁用，请联系管理员");
        }

        // 4. Sa-Token 登录
        StpUtil.login(user.getId());
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        return buildLoginUserVO(user);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public LoginUserVO getCurrentUser() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new NotLoginException("未登录", null, null);
        }
        return getUserById(userId);
    }

    @Override
    public LoginUserVO getUserById(Long userId) {
        if (userId == null) {
            throw new NotLoginException("未登录", null, null);
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("用户不存在: userId=" + userId);
        }
        return buildLoginUserVO(user);
    }

    @Override
    public Long getCurrentUserIdOrNull() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建 LoginUserVO：填充角色、部门、最大 dataScope、token。
     */
    private LoginUserVO buildLoginUserVO(SysUser user) {
        LoginUserVO vo = new LoginUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());

        // 查角色
        List<SysRole> roles = findActiveRolesByUserId(user.getId());
        DataScope maxScope = DataScope.SELF;
        for (SysRole role : roles) {
            RoleSimpleVO rs = new RoleSimpleVO();
            rs.setId(role.getId());
            rs.setCode(role.getCode());
            rs.setName(role.getName());
            vo.getRoles().add(rs);
            maxScope = DataScope.max(maxScope, DataScope.fromString(role.getDataScope()));
        }
        vo.setDataScope(maxScope.name());

        // 查部门
        List<SysDept> depts = findDeptsByUserId(user.getId());
        Map<Long, String> deptIdToName = collectDeptPathNames(depts);
        for (SysDept dept : depts) {
            DeptSimpleVO ds = new DeptSimpleVO();
            ds.setId(dept.getId());
            ds.setName(dept.getName());
            ds.setPath(buildDeptPath(dept, deptIdToName));
            vo.getDepts().add(ds);
        }

        // 当前 token（前端可读后塞到 header）
        try {
            vo.setToken(StpUtil.getTokenValue());
        } catch (Exception e) {
            // getCurrentUser 可能在某些上下文下拿不到 token，忽略
        }

        // 用户档案（user_profile），查不到时为 null（优雅降级）
        SysUserProfile profile = sysUserProfileMapper.selectOne(
                new LambdaQueryWrapper<SysUserProfile>().eq(SysUserProfile::getUserId, user.getId()));
        if (profile != null) {
            UserProfileVO pv = new UserProfileVO();
            pv.setRealName(profile.getRealName());
            pv.setIdCard(profile.getIdCard());
            pv.setAge(profile.getAge());
            pv.setEducation(profile.getEducation());
            pv.setHomeAddress(profile.getHomeAddress());
            vo.setProfile(pv);
        }

        return vo;
    }

    /**
     * 查用户的所有 ACTIVE 角色。
     */
    private List<SysRole> findActiveRolesByUserId(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, STATUS_ACTIVE));
    }

    /**
     * 查用户的所有部门。
     */
    private List<SysDept> findDeptsByUserId(Long userId) {
        List<SysUserDept> userDepts = sysUserDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId));
        if (userDepts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> deptIds = userDepts.stream().map(SysUserDept::getDeptId).toList();
        return sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .in(SysDept::getId, deptIds)
                        .eq(SysDept::getStatus, STATUS_ACTIVE));
    }

    /**
     * 收集这批部门的所有祖先 ID → 名称映射（含自己），用于构建完整路径。
     * 不带 status 过滤：祖先被禁用也要展示（路径完整性优先）。
     */
    private Map<Long, String> collectDeptPathNames(List<SysDept> depts) {
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = new HashSet<>();
        for (SysDept d : depts) {
            ids.add(d.getId());
            appendAncestorIds(d.getAncestors(), ids);
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysDeptMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
    }

    /**
     * 把 ancestors 字段（逗号分隔的 ID 链）里的正整数 ID 加入集合（0 是顶级占位，跳过）。
     */
    private void appendAncestorIds(String ancestors, Set<Long> target) {
        if (ancestors == null || ancestors.isBlank()) {
            return;
        }
        for (String token : ancestors.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            try {
                long aid = Long.parseLong(trimmed);
                if (aid > 0) target.add(aid);
            } catch (NumberFormatException ignored) {
                // 脏数据跳过
            }
        }
    }

    /**
     * 按 ancestors 顺序拼出"顶级 → 当前部门"的完整路径，最后一项是当前部门名。
     */
    private List<String> buildDeptPath(SysDept dept, Map<Long, String> idToName) {
        List<String> path = new ArrayList<>();
        if (dept.getAncestors() != null) {
            for (String token : dept.getAncestors().split(",")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    long aid = Long.parseLong(trimmed);
                    if (aid <= 0) continue;
                    String name = idToName.get(aid);
                    if (name != null && !name.isEmpty()) {
                        path.add(name);
                    }
                } catch (NumberFormatException ignored) {
                    // 脏数据跳过
                }
            }
        }
        // 当前部门名兜底（即使 idToName 没查到也要展示自身）
        if (dept.getName() != null && !dept.getName().isEmpty()) {
            path.add(dept.getName());
        }
        return path;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
