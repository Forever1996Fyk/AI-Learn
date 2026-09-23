package com.forever1996Fyk.ai.agentplus.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * @program: AI-Learn
 * @description: 多模态识别服务（图片 → 文本描述）。
 * @author: YuKai Fan
 * @create: 2026/9/22 10:49
 **/
@Slf4j
@Service
public class MultimodalService {
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${multimodal.model:qwen-vl-max}")
    private String model;

    @Value("${multimodal.temperature:0.2}")
    private double temperature;

    /**
     * HTTP 请求总超时（含连接 + 读取），默认 60s
     */
    @Value("${multimodal.timeout-seconds:60}")
    private long timeoutSeconds;

    private OpenAiChatModel multimodalChatModel;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(factory);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        multimodalChatModel = OpenAiChatModel.builder()
                .openAiApi(
                        OpenAiApi.builder()
                                .restClientBuilder(restClientBuilder)
                                .baseUrl(baseUrl)
                                .apiKey(new SimpleApiKey(apiKey))
                                .build()
                )
                .defaultOptions(options)
                .build();
        log.info("[shushu-agent-plus] 多模态模型初始化完成: {} ({}) | timeout={}s", model, baseUrl, timeoutSeconds);
    }

    /**
     * 调用多模态模型识别图片字节流，输出纯文本描述。
     *
     * @param bytes    图片字节（来自 MinIO 下载）
     * @param fileName 原始文件名（仅用于日志和 MIME 推断）
     */
    public String imageToText(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            return "[图片内容为空]";
        }
        try {
            MimeType mime = detectMimeType(fileName);
            ByteArrayResource resource = new ByteArrayResource(bytes);

            UserMessage userMessage = UserMessage.builder()
                    .text("请精简且全面的描述这张图片的内容，包括场景、对象、布局、颜色、文字信息，直接输出纯文本描述，不要多余解释和说明，不要使用换行符或特殊符号")
                    .media(List.of(new Media(mime, resource)))
                    .build();

            String resp = multimodalChatModel.call(new Prompt(List.of(userMessage))).getResult().getOutput().getText();

            if (resp == null || resp.trim().isEmpty()) {
                return "[无法识别图片内容]";
            }
            log.info("[dodo-agentx] 图片识别完成: {} | 描述长度={}", fileName, resp.length());
            return resp.trim();
        } catch (Exception e) {
            log.error("[dodo-agentx] 图片识别失败: {}", fileName, e);
            throw new RuntimeException("图片识别失败: " + e.getMessage(), e);
        }
    }

    private MimeType detectMimeType(String fileName) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png")) {
                return MimeTypeUtils.IMAGE_PNG;
            }
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return MimeTypeUtils.IMAGE_JPEG;
            }
            if (lower.endsWith(".gif")) {
                return MimeTypeUtils.IMAGE_GIF;
            }
            if (lower.endsWith(".bmp")) {
                return MimeType.valueOf("image/bmp");
            }
            if (lower.endsWith(".webp")) {
                return MimeType.valueOf("image/webp");
            }
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }
}
