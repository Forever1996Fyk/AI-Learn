package com.forever1996Fyk.ai.springai.mcpserver.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/21 10:25
 **/
@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final static String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${tools.open-weather-api-key}")
    private String apiKey = "8154255046098978efc2b96798dd207a";

    /**
     * 获取指定城市的天气情况
     *
     * @param city 城市名称，如"Beijing"
     * @return 天气文本信息
     * @Tool注解就是指定方法为 mcp 的工具，description就是大模型的提示词。
     * @ToolParam注解就是调用大模型时的参数，description就是参数的提示词。 这里还记得我们之前学习使用 tool的 json schema 吗？其中格式如下：
     * <p>
     * {
     * "name":"get_weather",
     * "description":"使用 OpenWeatherMap API查询指定城市的天气情况，返回天气信息报告",
     * "input_schema":{
     * "type":"object",
     * "properties":{
     * "city":{
     * "type":"string",
     * "description":"城市名称，比如 必须是英文格式，London或 Beijing"
     * }
     * },
     * "required":["city"]
     * }
     * }
     * 这里@Tool 和 @ToolParam注解就是分别对应其中的 tool的定义 和 properties 中的字段
     */
    @Tool(name = "get_weather", description = "获取指定城市的当前天气情况，格式化后的天气报告字符串")
    public String getWeather(@ToolParam(description = "城市名称，必须是英文格式，比如 London 或 Beijing") String city) {
        log.info("======调用了 getWeather工具=====");

        try {
            String charset = "UTF-8";
            String query = String.format("q=%s&appid=%s&units=metric&lang=zh_cn",
                    URLEncoder.encode(city, charset),
                    URLEncoder.encode(apiKey, charset));

            HttpClient httpClient = HttpClient.newBuilder()
                    .build();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(BASE_URL + "?" + query))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            System.out.println(body);
            JsonNode data = OBJECT_MAPPER.readTree(body);
            JsonNode main = data.get("main");
            JsonNode weatherArray = data.get("weather");
            JsonNode weather = weatherArray.get(0);
            JsonNode wind = data.get("wind");

            String description = weather.get("description").asText("无描述");
            double temp = main.get("temp").asDouble(Double.NaN);
            double feelsLike = main.get("feels_like").asDouble(Double.NaN);
            double tempMin = main.get("temp_min").asDouble(Double.NaN);
            double tempMax = main.get("temp_max").asDouble(Double.NaN);
            int pressure = main.get("pressure").asInt(0);
            int humidity = main.get("humidity").asInt(0);
            double windSpeed = wind.get("speed").asDouble(Double.NaN);
            return String.format("""
                    城市: %s
                    天气描述；%s
                    当前温度：%s
                    体感温度：%s
                    最低温度：%s
                    最高温度：%s
                    气压：%s
                    湿度：%s
                    风速：%.1f m/s
                    """, data.get("name").asText(city),
                    description,
                    temp,
                    feelsLike,
                    tempMin,
                    tempMax,
                    pressure,
                    humidity,
                    windSpeed);
        } catch (Exception e) {
            return "获取天气信息时出错：" + e.getMessage();
        }
    }

    public static void main(String[] args) {
        WeatherService weatherService = new WeatherService();
        System.out.println(weatherService.getWeather("Beijing"));
    }
}
