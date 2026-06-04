package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentSplitParam;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentUploadParam;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;

import java.io.IOException;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 11:01
 **/
public interface DocumentProcessService {

    /**
     * 上传文档
     *
     * @param documentUploadParam 文档上传参数
     * @return 知识文档实体
     * @throws IOException IO异常
     */
    KnowledgeDocumentEntity upload(DocumentUploadParam documentUploadParam) throws IOException;

    /**
     * 分割文档
     *
     * @param document        知识文档实体
     * @param documentSplitParam 文档分割参数
     * @return 分割结果
     */
    int split(KnowledgeDocumentEntity document, DocumentSplitParam documentSplitParam);

    /**
     * 嵌入文档并存储
     *
     * @param document 知识文档实体
     * @return 嵌入结果
     */
    boolean embedAndStore(KnowledgeDocumentEntity document);
}
