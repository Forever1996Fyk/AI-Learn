package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import reactor.core.publisher.Sinks;

/**
 * @program: AI-Learn
 * @description: PPT状态策略接口 使用策略模式处理不同状态的处理逻辑
 * @author: YuKai Fan
 * @create: 2026/6/24 23:34
 **/
public interface PptStateStrategy {

    /**
     * 执行该状态的处理逻辑
     *
     * @param inst           PPT实例
     * @param sink           响应流
     * @param query          用户查询
     * @param thinkingBuffer 思考缓冲区
     * @param context        策略上下文
     */
    void execute(AiPptInstEntity inst, Sinks.Many<String> sink, String query,
                 StringBuilder thinkingBuffer, PptStateStrategyContext context);

    /**
     * 获取该策略对应的目标状态
     * 执行成功后，状态应该变为这个状态
     *
     * @return 目标状态
     */
    PptInstStatus getTargetStatus();
}
