package com.forever1996Fyk.ai.agent.domain.record;

/**
 * @program: AI-Learn
 * @description: 批评结果记录
 * @author: YuKai Fan
 * @create: 2026/6/30 23:19
 **/
public record CritiqueResult(
        boolean passed,
        String feedback
) {
}
