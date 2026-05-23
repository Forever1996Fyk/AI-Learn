package com.forever1996Fyk.ai.springai.toolcalling.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/22 23:23
 **/
public class TimeTools {

    @Tool(description = "Get Time by zone id")
    public String getTimeByZoneId(@ToolParam(description = "Time zone id, such as Asia/Shanghai") String zoneId) {
        System.out.println("getTimeByZoneId, zonId=" + zoneId);
        ZoneId zid = ZoneId.of(zoneId);
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zid);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        return zonedDateTime.format(formatter);
    }
}
