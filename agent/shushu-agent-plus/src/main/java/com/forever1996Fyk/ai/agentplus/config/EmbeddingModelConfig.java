package com.forever1996Fyk.ai.agentplus.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * 手动构建 EmbeddingModel。
 */
@Slf4j
@Configuration
@ConditionalOnClass(OpenAiApi.class)
public class EmbeddingModelConfig {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String model;

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel() {
        return buildEmbeddingModel();
    }

    private OpenAiEmbeddingModel buildEmbeddingModel() {
        // 连接池 + idle 连接自动清理：复用热连接保证性能，清理死连接规避 Connection reset
        ConnectionProvider provider = ConnectionProvider.builder("embedding-pool")
                .maxIdleTime(Duration.ofSeconds(240))
                .evictInBackground(Duration.ofSeconds(30))
                .lifo()
                .build();
        // DefaultAddressResolverGroup：走 JDK DNS（用 OS 缓存），避免 Netty 默认 UDP 查询在 Windows 上的长延迟
        HttpClient httpClient = HttpClient.create(provider)
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .responseTimeout(Duration.ofSeconds(300));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .restClientBuilder(RestClient.builder()
                        .requestFactory(new ReactorClientHttpRequestFactory(httpClient)))
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();

        log.info("[dodo-agentx] EmbeddingModel 构建成功");
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }
}
