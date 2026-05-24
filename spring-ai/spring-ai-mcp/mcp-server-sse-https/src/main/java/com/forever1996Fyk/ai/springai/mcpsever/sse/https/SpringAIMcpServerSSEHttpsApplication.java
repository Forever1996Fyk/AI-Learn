package com.forever1996Fyk.ai.springai.mcpsever.sse.https;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/24 21:15
 **/
@SpringBootApplication
public class SpringAIMcpServerSSEHttpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAIMcpServerSSEHttpsApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTool(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }
}
