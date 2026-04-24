package com.forever1996Fyk.ai.springai.toolcalling.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.LocalDateTime;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/24 11:01
 **/
@Component
public class MyTools {
    private static final Logger log = LoggerFactory.getLogger(MyTools.class);

    @Tool(name = "getCurrentDateTime", description = "返回当前系统时间")
    public String getCurrentDateTime() {
        log.info("调用 getCurrentDateTime 工具");
        return LocalDateTime.now().toString();
    }

    @Tool(name = "calculate", description = "对两个数字执行加(add)、减(subtract)、乘(multiply)、除(divide)运算")
    public double calculate(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b,
            @ToolParam(description = "运算符") String operator
    ) {
        log.info("调用 calculate 工具，第一个参数 a={}, 第二个参数 b={}, 操作符={}", a, b, operator);
        return switch (operator) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> a / b;
            default -> 0.0;
        };
    }

    @Tool(name = "getOilPriceJson", description = "查询指定省份的油价，直接返回完整 JSON 字符串")
    public String getOilPriceJson(
            @ToolParam(description = "省份名称") String province
    ) {
        log.info("调用 getOilPriceJson 工具，省份名称={}", province);
        String api = "https://shanhe.kim/api/youjia/youjia.php?province=%s";
        String url = String.format(api, province);
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, String.class);
    }
}
