package com.forever1996Fyk.ai.agent.context;

import java.util.Set;

/**
 * @program: AI-Learn
 * @description: 上下文压缩策略配置。
 * 控制 Agent 循环中上下文压缩的行为，包括 token 阈值、保留数量、保护工具列表等。
 * @author: YuKai Fan
 * @create: 2026/7/6 22:33
 **/
public record ContextPolicy(
        int tokenThreshold,
        int keepRecentTools,
        int maxToolLength,
        Set<String> protectedTools
) {

    /**
     * 默认 token 阈值
     */
    public static final int DEFAULT_TOKEN_THRESHOLD = 60000;
    /**
     * 默认保留最近工具调用轮数
     */
    public static final int DEFAULT_KEEP_RECENT_TOOLS = 4;
    /**
     * 默认工具内容压缩阈值（ToolResponse 和 ToolCall args 统一使用）
     */
    public static final int DEFAULT_MAX_TOOL_LENGTH = 200;
    /**
     * 内置保护工具
     */
    private static final Set<String> BUILTIN_PROTECTED_TOOLS = Set.of("Skill");

    public static ContextPolicy defaults() {
        return new ContextPolicy(
                DEFAULT_TOKEN_THRESHOLD,
                DEFAULT_KEEP_RECENT_TOOLS,
                DEFAULT_MAX_TOOL_LENGTH,
                Set.of()
        );
    }

    public boolean isProtected(String toolName) {
        return protectedTools.contains(toolName);
    }
}
