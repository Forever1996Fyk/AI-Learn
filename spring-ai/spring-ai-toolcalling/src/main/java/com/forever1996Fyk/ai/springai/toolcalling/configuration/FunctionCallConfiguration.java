package com.forever1996Fyk.ai.springai.toolcalling.configuration;

import com.forever1996Fyk.ai.springai.toolcalling.tools.TimeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/22 23:15
 **/
@Configuration
public class FunctionCallConfiguration {

    @Bean
    @Description("根据用户输入的时区获取该时区的当前时间")
    public Function<TimeService.Request, TimeService.Response> getTimeFunction(TimeService timeService) {
        return timeService::getTimeByZoneId;
    }
}
