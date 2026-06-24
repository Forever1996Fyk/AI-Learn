package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.prompts.PptBuilderPrompts;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 23:34
 **/
@Slf4j
public class PptStateStrategyFactory {

    private static final Map<PptInstStatus, PptStateStrategy> STRATEGY_MAP = Maps.newConcurrentMap();

    private PptStateStrategyFactory() {
        // 私有构造函数，防止实例化
    }

    static {
        // 初始化策略映射
        // 顺序为：初始化 -> 需求澄清 -> 模板选择 -> 大纲生成 -> 信息收集 -> Schema生成 -> PPT渲染 -> 成功
        // 其中任意流程执行失败都会调用 失败策略
        STRATEGY_MAP.put(PptInstStatus.INIT, new RequirementStrategy());
        STRATEGY_MAP.put(PptInstStatus.REQUIREMENT, new RequirementStrategy());
        STRATEGY_MAP.put(PptInstStatus.TEMPLATE, new TemplateStrategy());
        STRATEGY_MAP.put(PptInstStatus.OUTLINE, new OutlineStrategy());
        STRATEGY_MAP.put(PptInstStatus.SEARCH, new SearchStrategy());
        STRATEGY_MAP.put(PptInstStatus.SCHEMA, new SchemaStrategy());
        STRATEGY_MAP.put(PptInstStatus.RENDER, new RenderStrategy());
        STRATEGY_MAP.put(PptInstStatus.SUCCESS, new SuccessStrategy());
        STRATEGY_MAP.put(PptInstStatus.FAILED, new FailedStrategy());
    }

    /**
     * 获取工厂实例
     */
    public static PptStateStrategyFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 获取指定状态的策略
     */
    public PptStateStrategy getStrategy(PptInstStatus status) {
        PptStateStrategy strategy = STRATEGY_MAP.get(status);
        if (strategy == null) {
            log.warn("未找到状态对应的策略: {}", status);
            return new DefaultStrategy();
        }
        return strategy;
    }

    /**
     * 执行下一个状态
     */
    public void executeNextState(AiPptInstEntity inst, Sinks.Many<String> sink, String query,
                                 StringBuilder thinkingBuffer, PptStateStrategyContext context) {
        try {
            // 重新加载最新状态
            AiPptInstEntity latestInst = context.getPptInstService().getById(inst.getId());
            if (latestInst != null) {
                inst = latestInst;
            }

            // 检查是否有错误信息，如果有则说明是断点重连
            assert latestInst != null;
            if (latestInst.getErrorMsg() != null && !latestInst.getErrorMsg().isEmpty()
                    && latestInst.getStatusEnum() != PptInstStatus.SUCCESS) {
                log.info("检测到断点重连: status={}, errorMsg={}",
                        latestInst.getStatusEnum(), latestInst.getErrorMsg());
                // 清空错误信息，允许继续执行
                context.getPptInstService().updateError(latestInst.getId(), "", latestInst.getStatusEnum());
            }

            PptInstStatus status = inst.getStatusEnum();
            log.info("状态机执行: status={}", status);

            PptStateStrategy strategy = getStrategy(status);
            strategy.execute(inst, sink, query, thinkingBuffer, context);
        } catch (Exception e) {
            log.error("继续状态机执行失败", e);
            sink.tryEmitError(e);
        }
    }

    /**
     * 执行 FAILED 状态策略
     * 统一处理失败场景，避免各策略重复创建 FailedStrategy 实例
     */
    public void executeFailedState(AiPptInstEntity inst, Sinks.Many<String> sink, String query,
                                   StringBuilder thinkingBuffer, PptStateStrategyContext context) {
        PptStateStrategy failedStrategy = getStrategy(PptInstStatus.FAILED);
        failedStrategy.execute(inst, sink, query, thinkingBuffer, context);
    }

    public void executeSchemaStrategy(AiPptInstEntity inst, Sinks.Many<String> sink, String question, StringBuilder thinkingBuffer, PptStateStrategyContext strategyContext) {
        SchemaStrategy schemaStrategy = new SchemaStrategy();
        String modifyPrompt = PptBuilderPrompts.getSchemaModifyPrompt(question, inst.getPptSchema());
        schemaStrategy.executeWithModifyPrompt(inst, sink, question, thinkingBuffer, strategyContext, modifyPrompt);
    }

    /**
     * 单例持有者
     */
    private static class SingletonHolder {
        private static final PptStateStrategyFactory INSTANCE = new PptStateStrategyFactory();
    }

    /**
     * 默认策略，用于处理未知状态
     */
    private static class DefaultStrategy implements PptStateStrategy {
        @Override
        public void execute(AiPptInstEntity inst, Sinks.Many<String> sink, String query,
                            StringBuilder thinkingBuffer, PptStateStrategyContext context) {
            log.warn("未知状态: {}", inst.getStatusEnum());
            sink.tryEmitNext(context.createThinkingResponse("❌ 状态异常，终止执行\n"));
            sink.tryEmitComplete();
        }

        @Override
        public PptInstStatus getTargetStatus() {
            return PptInstStatus.FAILED;
        }
    }
}
