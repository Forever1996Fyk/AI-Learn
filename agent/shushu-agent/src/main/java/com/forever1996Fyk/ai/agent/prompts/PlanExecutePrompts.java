package com.forever1996Fyk.ai.agent.prompts;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/27 22:57
 **/
public final class PlanExecutePrompts {
    private PlanExecutePrompts() {
    }

    /**
     * 获取当前系统时间
     * 时间信息作为独立的上下文注入，不包含在提示词模板中
     */
    public static String getCurrentTime() {
        return "当前正确的系统时间：" + LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
