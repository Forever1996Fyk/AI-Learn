package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import reactor.core.publisher.Sinks;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 23:49
 **/
public class OutlineStrategy implements PptStateStrategy {
    private static final PptInstStatus TARGET_STATUS = PptInstStatus.SCHEMA;

    @Override
    public void execute(AiPptInstEntity inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer, PptStateStrategyContext context) {

    }

    @Override
    public PptInstStatus getTargetStatus() {
        return TARGET_STATUS;
    }
}
