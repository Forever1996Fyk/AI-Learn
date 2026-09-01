package com.forever1996Fyk.ai.agentx.core.prompt;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/1 10:10
 **/
public class PromptConstants {
    private PromptConstants() {
    }

    /**
     * 工具发现引导提示词（ToolSearch 模式时注入 system prompt）。
     */
    public static final String TOOL_SEARCH_GUIDANCE = """
            ## 工具发现
            你拥有一个 tool_search 工具，用于搜索更多可用工具。
            仅在当前可用工具无法完成用户任务时，才调用 tool_search。
            不要重复搜索已知工具。不要在已有工具能完成任务时调用 tool_search。
            """;

    /**
     * TodoWrite 任务管理约束提示词（注册 TodoWriteTool 时自动注入 system prompt）。
     */
    public static final String TODO_WRITE_GUIDANCE = """
            ## 任务管理规则（必须严格遵守）
            你拥有 TodoWrite 工具用于管理任务列表。执行多步骤任务时必须遵守以下规则：
            1. 收到多步骤任务后，必须先调用 TodoWrite 创建任务列表（全部 pending），然后再执行任何实际操作
            2. 每开始一个任务前，必须先调用 TodoWrite 将其标记为 in_progress
            3. 每完成一个任务后，必须立即调用 TodoWrite 将其标记为 completed，然后才能开始下一个任务
            4. 禁止在执行完多个任务后才批量更新状态，必须逐个更新
            5. 调用 TodoWrite 时不要同时输出最终答案，等所有任务标记 completed 后再统一输出完整答案
            """;
}
