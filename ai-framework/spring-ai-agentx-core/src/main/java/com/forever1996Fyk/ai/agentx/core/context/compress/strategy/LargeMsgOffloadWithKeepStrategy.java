package com.forever1996Fyk.ai.agentx.core.context.compress.strategy;

import com.forever1996Fyk.ai.agentx.core.context.compress.CompressionContext;

/**
 * @program: AI-Learn
 * @description:
 * L2 大消息 offload（保护 lastKeep）。
 * 扫描历史轮次区域，但最近 lastKeep 条消息不参与（与 L1 同边界）。
 * @author: YuKai Fan
 * @create: 2026/9/20 10:20
 **/
public class LargeMsgOffloadWithKeepStrategy extends AbstractLargeMsgOffloadStrategy {
    @Override
    protected int scanEnd(CompressionContext ctx) {
        return ctx.historicalScanEnd(ctx.policy().lastKeep());
    }

    @Override
    public String name() {
        return "L2-LargeMsgOffload-WithKeep";
    }
}
