package com.forever1996Fyk.ai.agentplus.auth.service;

import com.forever1996Fyk.ai.agentplus.auth.dto.LoginRequest;
import com.forever1996Fyk.ai.agentplus.auth.dto.LoginUserVO;

/**
 * 认证服务接口。
 */
public interface AuthService {

    /**
     * 用户名密码登录。
     *
     * @return 登录用户完整信息（含 token）
     */
    LoginUserVO login(LoginRequest request);

    /**
     * 登出当前会话。
     */
    void logout();

    /**
     * 获取当前登录用户完整信息（角色 + 部门 + 数据范围）。
     * 依赖 Sa-Token ThreadLocal，仅能在 web 线程上调用。
     */
    LoginUserVO getCurrentUser();

    /**
     * 获取当前登录用户 ID（未登录返回 null）。
     * 依赖 Sa-Token ThreadLocal，仅能在 web 线程上调用。
     */
    Long getCurrentUserIdOrNull();

    /**
     * 按 userId 查询用户完整信息（角色 + 部门 + 数据范围）。
     * 纯数据库查询，不依赖 Sa-Token，可在异步线程上调用。
     */
    LoginUserVO getUserById(Long userId);
}
