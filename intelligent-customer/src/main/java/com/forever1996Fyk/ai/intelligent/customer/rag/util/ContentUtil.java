package com.forever1996Fyk.ai.intelligent.customer.rag.util;

import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/7 23:32
 **/
public final class ContentUtil {
    private ContentUtil() {
    }

    /**
     * 将内容标记为跳过重排序/融合
     * <p>
     * SQL/Cypher 等结构化查询结果带有该标记后，聚合器会直接透传，不再进行 RRF 融合和 scoring model 重排序。
     *
     * @param content 原始内容
     * @return 带有 skipRerank 标记的内容
     */
    public static Content markAsSkipRerank(Content content) {
        TextSegment originalSegment = content.textSegment();
        Metadata metadata = originalSegment.metadata() != null ?
                Metadata.from(originalSegment.metadata().toMap())
                : new Metadata();
        metadata.put(MetadataKeyConstant.SKIP_RERANK, "true");
        return Content.from(TextSegment.from(originalSegment.text(), metadata), content.metadata());
    }
}
