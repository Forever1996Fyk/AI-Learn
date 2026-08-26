package com.forever1996Fyk.ai.aiframework.agentscope.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/26 21:57
 **/
public class SimpleTools {

    @Tool(name = "get_time", description = "获取当前时间")
    public String getTime(
            @ToolParam(name = "zone", description = "时区，例如：北京") String zone
    ) {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
