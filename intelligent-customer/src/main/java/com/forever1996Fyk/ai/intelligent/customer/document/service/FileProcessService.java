package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.FileType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;

import java.io.InputStream;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 11:13
 **/
public interface FileProcessService {

    /**
     * Process the document.
     *
     * 1. 从 MinIO下载文件
     * 2. 调用文档解析接口获取 md/zip
     * 3. 转换后的文档保存在 MiniIO上
     * 4. 更新文档状态和转换后的 URL
     *
     * @param document    the document entity
     * @param inputStream the input stream of the document
     */
    String processDocument(KnowledgeDocumentEntity document, InputStream inputStream);


    /**
     * Supports the file type.
     *
     * @param fileType the file type
     * @param baseType the base type
     * @return the boolean
     */
    boolean supports(FileType fileType, KnowledgeBaseType baseType);
}


