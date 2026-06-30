package com.forever1996Fyk.ai.agent.domain.record;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/30 23:02
 **/
public record TaskResult(
        String taskId,
        boolean success,
        String output,
        String error
) {
}
