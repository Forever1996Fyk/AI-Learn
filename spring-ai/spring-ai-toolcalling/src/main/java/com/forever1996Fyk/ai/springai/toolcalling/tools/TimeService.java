package com.forever1996Fyk.ai.springai.toolcalling.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/22 23:16
 **/
@Service
public class TimeService {

    public Response getTimeByZoneId(Request request) {
        System.out.println("getTimeByZoneId, zonId=" + request.zonId());
        ZoneId zoneId = ZoneId.of(request.zonId());
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        return new Response(zonedDateTime.format(formatter));
    }

    public record Request(@JsonProperty(required = true,value = "zoneId")
                          @JsonPropertyDescription("时区, 比如 Asia/Shanghai") String zonId) {

    }

    public record Response(String time){}
}
