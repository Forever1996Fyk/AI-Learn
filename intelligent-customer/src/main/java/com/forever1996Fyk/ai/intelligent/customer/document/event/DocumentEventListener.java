package com.forever1996Fyk.ai.intelligent.customer.document.event;

import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 17:24
 **/
@Slf4j
@Component
public class DocumentEventListener {

    @Autowired
    private DocumentProcessService documentProcessService;
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    /**
     * 文档分块事件监听
     * 触发向量嵌入流程
     *
     * @param event 文档分块事件
     */
    @Async("documentEventListenerExecutor")
    @EventListener
    public void onDocumentChunked(DocumentChunkedEvent event) {
        Long documentId = event.getDocumentId();
        log.info("文档 CHUNKED 事件触发，开始执行向量嵌入，documentId: {}，segmentCount: {}", documentId, event.getSegmentCount());
        try {
            KnowledgeDocumentEntity document = knowledgeDocumentService.getById(documentId);
            boolean result = documentProcessService.embedAndStore(document);
            log.info("文档向量嵌入结果，documentId: {}，result: {}", documentId, result);
        } catch (Exception e) {
            log.error("文档向量嵌入失败，documentId: {}", documentId, e);
        }
    }
}
