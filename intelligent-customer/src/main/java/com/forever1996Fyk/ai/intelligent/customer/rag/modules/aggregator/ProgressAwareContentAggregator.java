package com.forever1996Fyk.ai.intelligent.customer.rag.modules.aggregator;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.query.Query;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/7 23:02
 **/
public class ProgressAwareContentAggregator implements ContentAggregator{

    private final ContentAggregator delegate;
    private final Consumer<String> progressCallback;

    public ProgressAwareContentAggregator(ContentAggregator delegate, Consumer<String> progressCallback) {
        Assert.notNull(delegate, "delegate must not be null");
        this.delegate = delegate;
        this.progressCallback = progressCallback;
    }

    @Override
    public List<Content> aggregate(Map<Query, Collection<List<Content>>> queryToContents) {
        // 发送进度：开始重排序/聚合
        if (progressCallback != null) {
            progressCallback.accept("[PROGRESS]:正在排序筛选结果...");
            System.out.println("[PROGRESS]:正在排序筛选结果...");
        }
        List<Content> result = delegate.aggregate(queryToContents);
        // 发送进度：聚合完成，即将进入LLM生成
        if (progressCallback != null) {
            progressCallback.accept("[PROGRESS]:正在生成回答...");
            System.out.println("[PROGRESS]:正在生成回答...");
        }
        return result;
    }
}
