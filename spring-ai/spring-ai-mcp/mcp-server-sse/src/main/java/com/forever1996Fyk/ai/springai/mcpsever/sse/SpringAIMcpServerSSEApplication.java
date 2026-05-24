package com.forever1996Fyk.ai.springai.mcpsever.sse;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/23 22:48
 **/
@SpringBootApplication
public class SpringAIMcpServerSSEApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAIMcpServerSSEApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTool(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }
}
