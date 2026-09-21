package com.forever1996Fyk.ai.agentplus.sys.service;

import cn.dev33.satoken.stp.StpUtil;
import com.forever1996Fyk.ai.agentplus.auth.dto.DeptSimpleVO;
import com.forever1996Fyk.ai.agentplus.auth.dto.RoleSimpleVO;
import com.forever1996Fyk.ai.agentplus.sys.dto.UserFormDTO;
import com.forever1996Fyk.ai.agentplus.sys.dto.UserProfileVO;
import com.forever1996Fyk.ai.agentplus.sys.dto.UserVO;
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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务（admin 才能调用，由 Sa-Token 拦截器在 /sys/** 路径上守卫）。
 * <p>
 * 多表操作（create/update/delete/assign*）加 @Transactional 保证原子性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String ADMIN_ROLE_CODE = "admin";
    /**
     * 内置超级管理员用户名，禁止删除（即便系统中还剩其它 admin）。
     * 用于防止误删初始 admin 账号导致登录名失效。
     */
    private static final String BUILTIN_ADMIN_USERNAME = "admin";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserProfileMapper sysUserProfileMapper;

    /**
     * 分页查询用户列表，支持 username/nickname 模糊搜索。
     */
    public IPage<UserVO> list(int page, int size, String keyword) {
        Page<SysUser> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getNickname, kw));
        }
        IPage<SysUser> userPage = sysUserMapper.selectPage(pageReq, wrapper);

        // 批量查角色/部门/档案，组装 VO
        List<SysUser> users = userPage.getRecords();
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        Map<Long, List<SysRole>> roleMap = batchFindRolesByUserIds(userIds);
        Map<Long, List<SysDept>> deptMap = batchFindDeptsByUserIds(userIds);
        Map<Long, SysUserProfile> profileMap = batchFindProfilesByUserIds(userIds);

        List<UserVO> voList = users.stream().map(u -> toVO(u, roleMap, deptMap, profileMap)).toList();
        Page<UserVO> result = new Page<>(pageReq.getCurrent(), pageReq.getSize(), pageReq.getTotal());
        result.setRecords(voList);
        return result;
    }

    /**
     * 新增用户。校验用户名唯一 + 明文密码 + 同步角色/部门关联。
     */
    @Transactional
    public UserVO create(UserFormDTO form) {
        if (form == null || isBlank(form.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (isBlank(form.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus(STATUS_ACTIVE);
        }

        // 用户名唯一性
        Long existCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, form.getUsername()));
        if (existCount != null && existCount > 0) {
            throw new IllegalArgumentException("用户名已存在：" + form.getUsername());
        }

        SysUser user = new SysUser();
        user.setUsername(form.getUsername());
        user.setPassword(form.getPassword());
        user.setNickname(form.getNickname());
        user.setStatus(form.getStatus());
        sysUserMapper.insert(user);

        syncUserRoles(user.getId(), form.getRoleIds());
        syncUserDepts(user.getId(), form.getDeptIds());
        upsertProfile(user.getId(), form.getProfile());

        log.info("新增用户: id={}, username={}", user.getId(), user.getUsername());
        return getUserVO(user.getId());
    }

    /**
     * 编辑用户基本信息 + 同步角色/部门。username 不能改。
     */
    @Transactional
    public UserVO update(Long id, UserFormDTO form) {
        if (id == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在：" + id);
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setNickname(form.getNickname());
        // status 字段不由编辑弹窗维护：未传则保持原状态，状态切换走专门的 toggle 接口
        if (form.getStatus() != null && !form.getStatus().isBlank()) {
            update.setStatus(form.getStatus());
        }

        // 密码明文入库（编辑弹窗始终回填，必填）
        if (!isBlank(form.getPassword())) {
            update.setPassword(form.getPassword());
        }
        sysUserMapper.updateById(update);

        if (form.getRoleIds() != null) {
            syncUserRoles(id, form.getRoleIds());
        }
        if (form.getDeptIds() != null) {
            syncUserDepts(id, form.getDeptIds());
        }
        upsertProfile(id, form.getProfile());

        log.info("编辑用户: id={}", id);
        return getUserVO(id);
    }

    /**
     * 删除用户（级联删 sys_user_role / sys_user_dept，不删 sys_role/sys_dept）。
     * 保护：
     * 1. 内置 admin 用户名禁止删除（避免误删导致无法以 admin/admin123 登录）
     * 2. 禁止删除最后一个 admin 角色账号，避免锁死系统
     */
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在：" + id);
        }

        // 保护 1：内置 admin 用户名禁止删除
        if (BUILTIN_ADMIN_USERNAME.equals(existing.getUsername())) {
            throw new IllegalStateException("内置超级管理员 admin 账号禁止删除");
        }

        // 保护 2：检查是否最后一个 admin
        Long adminRoleId = findAdminRoleId();
        if (adminRoleId != null) {
            Long adminUserCount = sysUserRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, adminRoleId));
            boolean isTargetAdmin = sysUserRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, id)
                            .eq(SysUserRole::getRoleId, adminRoleId)) > 0;
            if (isTargetAdmin && adminUserCount != null && adminUserCount <= 1) {
                throw new IllegalStateException("系统至少需要保留一个 admin 账号，禁止删除");
            }
        }

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        sysUserDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, id));
        sysUserMapper.deleteById(id);
        // 踢出该用户所有在线会话（避免删除后其 token 仍可用直到自然过期）
        kickoutIfLoggedIn(id);
        log.info("删除用户: id={}, username={}", id, existing.getUsername());
    }

    /**
     * 切换用户启用/禁用状态。
     * 保护：内置 admin 用户名禁止禁用（避免误操作锁死系统）。
     */
    public void changeStatus(Long userId, String status) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (!STATUS_ACTIVE.equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("状态值非法：" + status);
        }
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在：" + userId);
        }
        // 保护：内置 admin 禁止禁用
        if (BUILTIN_ADMIN_USERNAME.equals(existing.getUsername()) && "DISABLED".equals(status)) {
            throw new IllegalStateException("内置超级管理员 admin 账号禁止禁用");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(status);
        sysUserMapper.updateById(update);
        // 禁用时立即踢出该用户当前所有在线会话，否则其 token 仍可用直到自然过期
        if ("DISABLED".equals(status)) {
            kickoutIfLoggedIn(userId);
        }
        log.info("切换用户状态: id={}, username={}, status={}", userId, existing.getUsername(), status);
    }

    /**
     * 给用户重新分配角色（先删后插）。
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在：" + userId);
        }
        // 如果当前用户有 admin 角色，但新分配里去掉了 admin，且系统只剩这一个 admin，禁止
        Long adminRoleId = findAdminRoleId();
        if (adminRoleId != null && (roleIds == null || !roleIds.contains(adminRoleId))) {
            boolean isTargetAdmin = sysUserRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, userId)
                            .eq(SysUserRole::getRoleId, adminRoleId)) > 0;
            Long adminUserCount = sysUserRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, adminRoleId));
            if (isTargetAdmin && adminUserCount != null && adminUserCount <= 1) {
                throw new IllegalStateException("系统至少需要保留一个 admin 账号，禁止移除其 admin 角色");
            }
        }
        syncUserRoles(userId, roleIds == null ? Collections.emptyList() : roleIds);
    }

    /**
     * 给用户重新分配部门（先删后插）。
     */
    @Transactional
    public void assignDepts(Long userId, List<Long> deptIds) {
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在：" + userId);
        }
        syncUserDepts(userId, deptIds == null ? Collections.emptyList() : deptIds);
    }

    /**
     * 重置密码（明文入库）。
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (isBlank(newPassword)) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在：" + userId);
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(newPassword);
        sysUserMapper.updateById(update);
        log.info("重置用户密码: id={}, username={}", userId, existing.getUsername());
    }

    // ======================================================================
    // 私有工具方法
    // ======================================================================

    /**
     * 同步用户-角色关联表（先删后插）。
     */
    private void syncUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole rel = new SysUserRole();
            rel.setUserId(userId);
            rel.setRoleId(roleId);
            sysUserRoleMapper.insert(rel);
        }
    }

    /**
     * 同步用户-部门关联表（先删后插）。
     */
    private void syncUserDepts(Long userId, List<Long> deptIds) {
        sysUserDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId));
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        for (Long deptId : deptIds) {
            SysUserDept rel = new SysUserDept();
            rel.setUserId(userId);
            rel.setDeptId(deptId);
            sysUserDeptMapper.insert(rel);
        }
    }

    /**
     * 查 code=admin 的角色 ID（单条，缓存于本方法局部，无外部缓存）。
     */
    private Long findAdminRoleId() {
        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, ADMIN_ROLE_CODE));
        return adminRole == null ? null : adminRole.getId();
    }

    /**
     * 查用户角色/部门/档案并组装 VO。
     */
    private UserVO getUserVO(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("用户不存在：" + userId);
        }
        Map<Long, List<SysRole>> roleMap = batchFindRolesByUserIds(List.of(userId));
        Map<Long, List<SysDept>> deptMap = batchFindDeptsByUserIds(List.of(userId));
        Map<Long, SysUserProfile> profileMap = batchFindProfilesByUserIds(List.of(userId));
        return toVO(user, roleMap, deptMap, profileMap);
    }

    private UserVO toVO(SysUser user,
                        Map<Long, List<SysRole>> roleMap,
                        Map<Long, List<SysDept>> deptMap,
                        Map<Long, SysUserProfile> profileMap) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPassword(user.getPassword());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());

        List<SysRole> roles = roleMap.getOrDefault(user.getId(), Collections.emptyList());
        for (SysRole role : roles) {
            RoleSimpleVO rs = new RoleSimpleVO();
            rs.setId(role.getId());
            rs.setCode(role.getCode());
            rs.setName(role.getName());
            vo.getRoles().add(rs);
        }

        List<SysDept> depts = deptMap.getOrDefault(user.getId(), Collections.emptyList());
        for (SysDept dept : depts) {
            DeptSimpleVO ds = new DeptSimpleVO();
            ds.setId(dept.getId());
            ds.setName(dept.getName());
            vo.getDepts().add(ds);
        }

        SysUserProfile profile = profileMap.get(user.getId());
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
     * 批量查询用户 ID → 档案。
     */
    private Map<Long, SysUserProfile> batchFindProfilesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserProfileMapper.selectList(
                        new LambdaQueryWrapper<SysUserProfile>().in(SysUserProfile::getUserId, userIds))
                .stream().collect(Collectors.toMap(SysUserProfile::getUserId, p -> p, (a, b) -> a));
    }

    /**
     * 档案 upsert：form 非空时按 userId 写入（存在则更新，不存在则插入）。
     * realName/idCard 至少一个非空才执行，避免空对象误写。
     */
    private void upsertProfile(Long userId, UserProfileVO form) {
        if (form == null) {
            return;
        }
        boolean hasValue = !isBlank(form.getRealName()) || !isBlank(form.getIdCard())
                || form.getAge() != null || !isBlank(form.getEducation())
                || !isBlank(form.getHomeAddress());
        if (!hasValue) {
            return;
        }
        SysUserProfile existing = sysUserProfileMapper.selectOne(
                new LambdaQueryWrapper<SysUserProfile>().eq(SysUserProfile::getUserId, userId));
        if (existing == null) {
            SysUserProfile entity = new SysUserProfile();
            entity.setUserId(userId);
            entity.setRealName(!isBlank(form.getRealName()) ? form.getRealName() : "");
            entity.setIdCard(!isBlank(form.getIdCard()) ? form.getIdCard() : "");
            entity.setAge(form.getAge());
            entity.setEducation(form.getEducation());
            entity.setHomeAddress(form.getHomeAddress());
            sysUserProfileMapper.insert(entity);
        } else {
            SysUserProfile update = new SysUserProfile();
            update.setId(existing.getId());
            update.setRealName(!isBlank(form.getRealName()) ? form.getRealName() : existing.getRealName());
            update.setIdCard(!isBlank(form.getIdCard()) ? form.getIdCard() : existing.getIdCard());
            update.setAge(form.getAge() != null ? form.getAge() : existing.getAge());
            update.setEducation(form.getEducation() != null ? form.getEducation() : existing.getEducation());
            update.setHomeAddress(form.getHomeAddress() != null ? form.getHomeAddress() : existing.getHomeAddress());
            sysUserProfileMapper.updateById(update);
        }
    }

    /**
     * 批量查询用户 ID → 角色列表。
     * 一次性查 sys_user_role + sys_role，避免 N+1。
     */
    private Map<Long, List<SysRole>> batchFindRolesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserRole> rels = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        if (rels.isEmpty()) {
            return Collections.emptyMap();
        }
        // roleId → SysRole
        List<Long> roleIds = rels.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, SysRole> roleById = sysRoleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds))
                .stream().collect(Collectors.toMap(SysRole::getId, r -> r));
        // userId → List<SysRole>
        return rels.stream().collect(Collectors.groupingBy(SysUserRole::getUserId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ur -> roleById.get(ur.getRoleId()))
                                .filter(java.util.Objects::nonNull)
                                .toList()));
    }

    /**
     * 批量查询用户 ID → 部门列表。
     */
    private Map<Long, List<SysDept>> batchFindDeptsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserDept> rels = sysUserDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().in(SysUserDept::getUserId, userIds));
        if (rels.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> deptIds = rels.stream().map(SysUserDept::getDeptId).distinct().toList();
        Map<Long, SysDept> deptById = sysDeptMapper.selectList(
                        new LambdaQueryWrapper<SysDept>().in(SysDept::getId, deptIds))
                .stream().collect(Collectors.toMap(SysDept::getId, d -> d));
        return rels.stream().collect(Collectors.groupingBy(SysUserDept::getUserId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ud -> deptById.get(ud.getDeptId()))
                                .filter(java.util.Objects::nonNull)
                                .toList()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * 踢出指定用户的所有在线会话（如果当前登录的话）。
     * 用于禁用/删除用户后让其 token 立即失效。
     * 未登录时调用 kickout 不会抛异常，但会留下无意义的日志，所以先 isLogin 判断一下。
     */
    private void kickoutIfLoggedIn(Long userId) {
        try {
            if (StpUtil.isLogin(userId)) {
                StpUtil.kickout(userId);
            }
        } catch (Exception e) {
            log.warn("[userService] 踢出用户会话失败，忽略: userId={}, err={}", userId, e.getMessage());
        }
    }
}
