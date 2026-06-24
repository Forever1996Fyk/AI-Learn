package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import reactor.core.publisher.Sinks;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 23:50
 **/
public class SchemaStrategy implements PptStateStrategy {

    private static final PptInstStatus TARGET_STATUS = PptInstStatus.RENDER;
    @Override
    public void execute(AiPptInstEntity inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer, PptStateStrategyContext context) {

    }

    @Override
    public PptInstStatus getTargetStatus() {
        return TARGET_STATUS;
    }

    /**
     * 执行 Schema 策略，支持修改模式
     *
     * @param inst PPT 实例
     * @param sink 输出 sink
     * @param question 用户查询
     * @param thinkingBuffer 思考缓冲
     * @param context 策略上下文
     * @param modifyPrompt 修改提示词，如果为 null 表示正常流程
     */
    public void executeWithModifyPrompt(AiPptInstEntity inst, Sinks.Many<String> sink, String question, StringBuilder thinkingBuffer, PptStateStrategyContext context, String modifyPrompt) {

    }
}
