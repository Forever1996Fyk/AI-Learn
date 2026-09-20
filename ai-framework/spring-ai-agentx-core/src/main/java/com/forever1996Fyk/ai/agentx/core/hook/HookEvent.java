package com.forever1996Fyk.ai.agentx.core.hook;

/**
 * @program: AI-Learn
 * @description:
 * Agent 生命周期事件基接口。
 *
 * <p>sealed 接口保证事件类型集合封闭，Hook 实现可用 exhaustive switch 模式匹配。
 * @author: YuKai Fan
 * @create: 2026/9/18 10:28
 **/
public sealed interface HookEvent permits
        BeforeCallEvent,
        AfterCallEvent,
        BeforeReasoningEvent,
        AfterReasoningEvent,
        BeforeToolExecutionEvent,
        AfterToolExecutionEvent,
        ErrorEvent {
}
