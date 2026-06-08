package com.forever1996Fyk.ai.intelligent.customer.rag.modules.aggregator;

import com.alibaba.fastjson2.JSON;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.RetrievalSource;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatMessageEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatMessageService;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import com.forever1996Fyk.ai.intelligent.customer.rag.util.ReferenceUtil;
import com.google.common.collect.Lists;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/7 23:02
 **/
@Slf4j
public class ProgressAwareContentAggregator implements ContentAggregator {

    private final ContentAggregator delegate;
    private final Consumer<String> progressCallback;
    private final String chatMessageId;
    private final ChatMessageService chatMessageService;

    public ProgressAwareContentAggregator(ContentAggregator delegate, String chatMessageId, ChatMessageService chatMessageService, Consumer<String> progressCallback) {
        Assert.notNull(delegate, "delegate must not be null");
        this.delegate = delegate;
        this.chatMessageId = chatMessageId;
        this.chatMessageService = chatMessageService;
        this.progressCallback = progressCallback;
    }

    @Override
    public List<Content> aggregate(Map<Query, Collection<List<Content>>> queryToContents) {
        // 发送进度：开始重排序/聚合
        if (progressCallback != null) {
            progressCallback.accept("[PROGRESS]:正在排序筛选结果...");
            System.out.println("[PROGRESS]:正在排序筛选结果...");
        }
        List<Content> results = delegate.aggregate(queryToContents);
        List<ChatMessageEntity.RagReference> ragReferencesDocs = Lists.newArrayList();
        try {
            ragReferencesDocs = results.stream()
                    .collect(Collectors.toMap(
                            content -> content.textSegment().metadata().getInteger(MetadataKeyConstant.DOC_ID),
                            content -> content,
                            (existing, replacement) -> existing
                    )).values().stream()
                    .map(content -> ReferenceUtil.getRagReference(content, RetrievalSource.HYBRID))
                    .toList();

            // chunk维度的RAG引用信息，用于数据持久化
            List<ChatMessageEntity.RagReference> ragReferenceChunks = results.stream()
                    .collect(Collectors.toMap(
                            content -> content.textSegment().metadata().getString(MetadataKeyConstant.CHUNK_ID),
                            content -> content,
                            (existing, replacement) -> existing
                    )).values().stream()
                    .map(content -> ReferenceUtil.getRagReference(content, RetrievalSource.HYBRID))
                    .toList();
            if (!CollectionUtils.isEmpty(ragReferenceChunks) && chatMessageService != null && chatMessageId != null) {
                chatMessageService.updateRagReferences(chatMessageId, ragReferenceChunks);
            }

            // 过滤掉chunkId为空的引用，一般是非知识库检索得到的结果
            ragReferencesDocs = ragReferencesDocs.stream().filter(reference -> reference.getChunkId() != null).toList();

        } catch (Exception e) {
            log.warn("RAG引用信息回写失败: assistantMsgId={}", chatMessageId, e);
        }
        // 发送进度：聚合完成，即将进入LLM生成
        if (progressCallback != null) {
            if (!CollectionUtils.isEmpty(ragReferencesDocs)) {
                progressCallback.accept("[REFERENCE]:" + JSON.toJSONString(ragReferencesDocs));
                System.out.println("[REFERENCE]:" + JSON.toJSONString(ragReferencesDocs));
            }
            progressCallback.accept("[PROGRESS]:正在生成回答...");
            System.out.println("[PROGRESS]:正在生成回答...");
        }
        return results;
    }
}
