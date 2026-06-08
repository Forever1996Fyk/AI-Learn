package com.forever1996Fyk.ai.intelligent.customer.rag.util;

import com.forever1996Fyk.ai.intelligent.customer.chat.enums.RetrievalSource;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatMessageEntity;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/8 21:37
 **/
public class ReferenceUtil {
    public static ChatMessageEntity.RagReference getRagReference(Content content, RetrievalSource retrievalSource) {
        Metadata metadata = content.textSegment().metadata();
        return ChatMessageEntity.RagReference.builder()
                .documentId(metadata.getInteger(MetadataKeyConstant.DOC_ID) + "")
                .documentTitle(metadata.getString(MetadataKeyConstant.FILE_NAME))
                .url(metadata.getString(MetadataKeyConstant.URL))
                .chunkId(metadata.getString(MetadataKeyConstant.CHUNK_ID))
                .chunkContent(content.textSegment().text())
                .similarityScore((Double) content.metadata().get(ContentMetadata.SCORE))
                .rerankScore((Double) content.metadata().get(ContentMetadata.RERANKED_SCORE))
                .retrievalSource(retrievalSource)
                .build();
    }
}
