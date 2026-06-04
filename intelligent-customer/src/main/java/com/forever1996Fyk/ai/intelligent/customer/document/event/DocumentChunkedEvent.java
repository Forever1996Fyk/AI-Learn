package com.forever1996Fyk.ai.intelligent.customer.document.event;

import org.springframework.context.ApplicationEvent;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 17:21
 **/
public class DocumentChunkedEvent extends ApplicationEvent {

    /**
     * 文档ID
     */
    private final Long documentId;

    /**
     * 分段数量
     */
    private final int segmentCount;

    public DocumentChunkedEvent(Object source, Long documentId, int segmentCount) {
        super(source);
        this.documentId = documentId;
        this.segmentCount = segmentCount;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    @Override
    public String toString() {
        return "DocumentChunkedEvent{documentId=" + documentId + ", segmentCount=" + segmentCount + '}';
    }
}
