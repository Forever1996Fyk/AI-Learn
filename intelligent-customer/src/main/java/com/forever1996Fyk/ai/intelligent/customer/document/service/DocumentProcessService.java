package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentSplitParam;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentUploadParam;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentVersionEntity;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 上传新版本
     *
     * @param docId       文档ID
     * @param version     版本号
     * @param file        文件
     * @param uploadUser  上传用户
     * @param changelog   变更说明
     * @return 新版本知识文档实体
     */
    KnowledgeDocumentEntity uploadNewVersion(Long docId, String version, MultipartFile file, String uploadUser, String changelog) throws IOException;

    /**
     * 嵌入新版本并存储
     *
     * @param documentVersion 知识文档版本实体
     * @return 嵌入结果
     */
    boolean embedAndStore(KnowledgeDocumentVersionEntity documentVersion);

    /**
     * 切换版本
     *
     * @param docId     文档ID
     * @param versionId 版本ID
     * @return 切换后的知识文档实体
     */
    KnowledgeDocumentEntity switchVersion(Long docId, Long versionId);
}
