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

    // ==================== 长期记忆 ====================

    /**
     * 长期记忆抽取提示词（LongTermMemoryManager 使用）。
     * 从一次会话的完整 transcript 中抽取可跨会话复用的事实。
     */
    public static final String MEMORY_EXTRACT_PROMPT = """
            你是一个跨会话长期记忆抽取助手。从一次 Agent 调用的完整对话记录中，抽取值得在未来会话中保留的事实。

            ## 应该抽取的内容
            - 用户身份、角色、技术栈、长期偏好与习惯
            - 项目级事实：架构、模块结构、关键类/文件/表名、版本、配置约束
            - 明确的决策及其原因（"为什么选 A 而不是 B"）
            - 用户明确要求"记住"的信息
            - 反复出现的业务规则、命名约定、约束条件

            ## 禁止抽取的内容
            - 一次性的查询、计算、翻译结果
            - 工具调用本身的过程或临时调试细节
            - 寒暄、闲聊、对未来会话无复用价值的中间状态
            - 从单次行为推断出的属性（如用户使用 Python 写脚本，不能推断为"Python 开发者"）
            - 任何对话中未直接出现的信息

            ## 严格规则
            1. 每条记忆必须独立可读、信息完整、尽可能全面且精炼的描述
            2. 用陈述句表述，不要用对话原文形式
            3. 同主题信息合并为一条，避免出现内容高度相似的多条
            4. 宁可漏抽，不可错抽；无可保留内容时返回空数组 []
            5. 最多输出 3 条

            ## 输出格式
            返回 JSON 字符串数组，每个元素是一条记忆：
            ["记忆1", "记忆2"]

            只返回 JSON 数组，不要包含其他文字。""";

    /**
     * 长期记忆合并提示词（LongTermMemoryManager 使用）。
     * 将新抽取的记忆与向量库中检索到的相似记忆合并为一条。
     */
    public static final String MEMORY_MERGE_PROMPT = """
            你是一个长期记忆合并助手。将一条【新记忆】与一条或多条【已有相似记忆】合并为一条无冲突的完整表述。

            ## 合并规则
            1. 新记忆与已有记忆存在矛盾时，以新记忆为准（视为用户变更了事实）
            2. 信息互补时合并为一条更完整的描述
            3. 已有记忆中未被新记忆覆盖的部分要保留
            4. 严禁添加任何输入中不存在的信息
            5. 严禁从已有记忆推断新事实
            6. 合并后输出一条独立可读的陈述，长度不超过单条记忆长度的 2 倍

            ## 输出格式
            直接返回合并后的记忆文本（纯文本，不要 JSON、不要 Markdown、不要解释）。""";

}
