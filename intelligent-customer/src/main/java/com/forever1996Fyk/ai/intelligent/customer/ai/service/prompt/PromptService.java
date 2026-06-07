package com.forever1996Fyk.ai.intelligent.customer.ai.service.prompt;

import com.forever1996Fyk.ai.intelligent.customer.ai.enums.IntelligentCustomerIntent;
import com.forever1996Fyk.ai.intelligent.customer.ai.model.IntentRecognitionResult;
import com.google.common.collect.Maps;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/7 21:33
 **/
@Service
public class PromptService {
    private final Map<IntelligentCustomerIntent, String> promptCache = Maps.newConcurrentMap();
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();


    /**
     * 根据意图获取提示词
     */
    public String getPrompt(IntelligentCustomerIntent intent) {
        return promptCache.computeIfAbsent(intent, this::loadPromptFromFile);
    }

    /**
     * 根据意图获取提示词
     */
    public String getPrompt(IntentRecognitionResult intent) {
        return promptCache.computeIfAbsent(IntelligentCustomerIntent.getIntent(intent), this::loadPromptFromFile);
    }

    /**
     * 从文件加载提示词（带缓存）
     */
    private String loadPromptFromFile(IntelligentCustomerIntent intent) {
        try {
            Resource resource = resolver.getResource("classpath:/prompts/" + intent.getFileName());
            return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            // 如果指定意图的文件不存在，返回默认提示词
            if (intent != IntelligentCustomerIntent.CAR_OTHER) {
                return getPrompt(IntelligentCustomerIntent.CAR_OTHER);
            }
            throw new RuntimeException("默认提示词文件缺失", e);
        }
    }
}
