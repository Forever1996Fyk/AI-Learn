package com.forever1996Fyk.ai.springai.mcpserver.config;

import com.forever1996Fyk.ai.springai.mcpserver.weather.WeatherService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/21 11:36
 **/
@Configuration
public class MCPServerConfig {

    @Bean
    public ToolCallbackProvider weatherProvider(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherService)
                .build();
    }
}
