package com.forever1996Fyk.ai.intelligent.customer.document.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 21:59
 **/
@Slf4j
@Component
public class DocumentCompensationJob {
    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;
    @Autowired
    private DocumentProcessService documentProcessService;
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 5;

//    @XxlJob("documentEmbeddingCompensation")
    public void documentEmbeddingCompensation() {
        log.info("开始执行向量化补偿任务");
        int successCount = 0;
        int failCount = 0;
        List<KnowledgeDocumentEntity> documents = knowledgeDocumentService.list(
                new QueryWrapper<KnowledgeDocumentEntity>().lambda()
                        .eq(KnowledgeDocumentEntity::getStatus, DocumentStatus.CHUNKED)
        );
        log.info("发现 {} 个 CHUNKED 状态的文档", documents.size());
        for (KnowledgeDocumentEntity document : documents) {
            try {
                int retryCount = getRetryCount(document);
                if (retryCount >= MAX_RETRY_COUNT) {
                    log.warn("文档 {} 已达最大重试次数 {}，跳过补偿", document.getDocId(), retryCount);
                    continue;
                }
                // 执行向量化
                boolean success = documentProcessService.embedAndStore(document);
                if (success) {
                    // 更新重试次数
                    updateRetryCount(document.getDocId(), retryCount);
                    log.info("向量化补偿成功，documentId: {}", document.getDocId());
                    successCount++;
                } else {
                    log.warn("向量化补偿失败，documentId: {}", document.getDocId());
                    failCount++;
                }
            } catch (Exception e) {
                log.error("向量化补偿失败，documentId: {}", document.getDocId(), e);
                failCount++;
            }
        }
        log.info("向量化补偿完成，成功：{}，失败：{}", successCount, failCount);
    }

    /**
     * 从 extension 字段获取重试次数
     */
    private int getRetryCount(KnowledgeDocumentEntity document) {
        String extension = document.getExtension();
        if (extension == null || extension.isEmpty()) {
            return 0;
        }
        try {
            JSONObject json = JSON.parseObject(extension);
            return json.getIntValue("retryCount");
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 更新重试次数到 extension 字段
     */
    private void updateRetryCount(Long documentId, int retryCount) {
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(documentId);
        if (document == null) {
            return;
        }

        JSONObject json;
        String extension = document.getExtension();
        if (extension == null || extension.isEmpty()) {
            json = new JSONObject();
        } else {
            try {
                json = JSON.parseObject(extension);
            } catch (Exception e) {
                json = new JSONObject();
            }
        }

        json.put("retryCount", retryCount);
        json.put("lastRetryTime", LocalDateTime.now().toString());

        document.setExtension(json.toJSONString());
        knowledgeDocumentService.updateById(document);
    }
}
