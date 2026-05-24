package com.forever1996Fyk.ai.springai.mcpsever.streamable;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/23 22:58
 **/
@SpringBootApplication
public class SpringAIMcpServerStreamableApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringAIMcpServerStreamableApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTool(WeatherService weatherService,
                                            GoodsService goodsService,
                                            TradeService tradeService,
                                            OrderService orderService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherService, goodsService, tradeService, orderService)
                .build();
    }
}
