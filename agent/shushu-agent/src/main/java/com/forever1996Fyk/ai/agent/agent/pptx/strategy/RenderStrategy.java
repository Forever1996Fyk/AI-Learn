package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * @program: AI-Learn
 * @description: PPT渲染策略
 * @author: YuKai Fan
 * @create: 2026/6/24 23:50
 **/
@Slf4j
public class RenderStrategy implements PptStateStrategy {

    private static final PptInstStatus TARGET_STATUS = PptInstStatus.SUCCESS;

    @Override
    public void execute(AiPptInstEntity inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer, PptStateStrategyContext context) {
        sink.tryEmitNext(context.createThinkingResponse("正在渲染PPT...\n"));

        Disposable disposable = Mono.fromCallable(() -> {
                    String pptSchemaJson = inst.getPptSchema();
                    return context.getPptRenderService().renderPpt(inst, pptSchemaJson);
                })
                .doOnSuccess(fileUrl -> {
                    context.getPptInstService().updateFileUrl(inst.getId(), fileUrl, TARGET_STATUS);
                    sink.tryEmitNext(context.createThinkingResponse("✅ PPT渲染完成\n"));
                    context.continueStateMachine(inst, sink, query, thinkingBuffer);
                })
                .doOnError(err -> {
                    log.error("PPT渲染异常", err);
                    // 失败时不回退状态，只更新错误信息，转到 FAILED
                    context.getPptInstService().updateError(inst.getId(),
                            "PPT渲染失败: " + err.getMessage(), PptInstStatus.RENDER);
                    // 转到 FAILED 策略
                    PptStateStrategyFactory.getInstance().executeFailedState(inst, sink, query, thinkingBuffer, context);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        // 保存 disposable 到任务管理器，用于停止任务
        context.setDisposable(inst.getConversationId(), disposable);
    }

    @Override
    public PptInstStatus getTargetStatus() {
        return TARGET_STATUS;
    }
}
