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
     * 文档的版本号
     */
    private final Long documentVersionId;

    /**
     * 分段数量
     */
    private final int segmentCount;

    public DocumentChunkedEvent(Object source, Long documentId, Long documentVersionId, int segmentCount) {
        super(source);
        this.documentId = documentId;
        this.documentVersionId = documentVersionId;
        this.segmentCount = segmentCount;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    public Long getDocumentVersionId() {
        return documentVersionId;
    }

    @Override
    public String toString() {
        return "DocumentChunkedEvent{documentId=" + documentId + ", segmentCount=" + segmentCount + '}';
    }
}
