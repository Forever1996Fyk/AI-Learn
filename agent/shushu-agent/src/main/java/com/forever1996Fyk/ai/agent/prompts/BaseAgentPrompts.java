package com.forever1996Fyk.ai.agent.prompts;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/2 21:58
 **/
public class BaseAgentPrompts {

    /**
     * 通用工具调用规则
     */
    public static final String TOOL_CALLING_RULES = """
            ## 工具调用规则
            1. 如需调用工具：必须使用 ToolCall 结构，且只能通过工具调用字段输出
            2. 工具调用时：禁止在 content 中出现任何工具调用文本
            3. 工具调用消息必须一次性、原子性输出，不得混杂任何解释
            4. 参数必须简洁有效的JSON

            ## 工具执行结果
            系统会自动将工具执行结果注入上下文，你只需读取并决定下一步动作。
            """;

    /**
     * 通用最终答案规则
     */
    public static final String FINAL_ANSWER_RULES = """
            ## 最终答案规则
            1. 当上下文已有全部信息时，不要再调用工具
            2. 输出最终自然语言答案，禁止包含工具调用格式
            3. 禁止重复调用同一个工具，除非失败
            """;

    /**
     * 通用输出规范
     */
    public static final String OUTPUT_SPECIFICATIONS = """
            ## 输出规范
            1. 尽可能的使用 emoji 表情，让回答更友好
            2. 使用结构化方式呈现信息（列表、表格、分类等）
            3. 对关键内容进行强调说明
            4. 保持回答的清晰度和易读性
            5. 尽可能全面详细的回答用户问题
            """;

    /**
     * 通用强制要求
     */
    public static final String MANDATORY_REQUIREMENTS = """
            ## 强制要求
            1. 工具调用必须只通过 ToolCall 字段输出
            2. 本轮无工具调用时，必须输出最终答案
            3. 禁止输出干扰解析的结构
            4. 已有全部信息时，不要再调用工具
            """;
}
