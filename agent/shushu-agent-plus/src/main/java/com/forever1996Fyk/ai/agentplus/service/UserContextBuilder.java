package com.forever1996Fyk.ai.agentplus.service;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/21 16:09
 **/
public interface UserContextBuilder {

    /**
     * 构建用户上下文
     *
     * @param userId 用户ID
     * @return 用户上下文
     */
    String build(Long userId);
}
