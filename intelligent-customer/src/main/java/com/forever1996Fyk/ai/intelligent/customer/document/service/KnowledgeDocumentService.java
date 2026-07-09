package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 知识文档表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocumentEntity> {

    /**
     * 删除文档及其分块
     *
     * @param docId 文档id
     */
    boolean removeDocumentWithSegments(Long docId);

    /**
     * 同步推进文档和指定版本的状态。
     * 仅当当前状态按生命周期顺序早于目标状态时才会更新；若当前状态已大于或等于目标状态，则跳过，避免状态回退。
     *
     * @param docId        文档ID
     * @param versionId    版本ID（knowledge_document_version.version_id）
     * @param targetStatus 目标状态，如 CONVERTING、CONVERTED、CHUNKED、VECTOR_STORED、STORED
     * @return 是否执行了更新（true：文档或版本至少有一个被更新；false：均未更新）
     */
    boolean advanceDocumentAndVersionStatus(Long docId, Long versionId, DocumentStatus targetStatus);

    /**
     * 让指定版本生效（重新向量化）：
     * 1. 校验版本状态必须为 CHUNKED
     * 2. 对该版本下所有 STORED 且未向量化的分段重新 embed 并写入 ES
     * 3. 将分段状态更新为 VECTOR_STORED
     * 4. 将版本记录状态从 CHUNKED 升为 VECTOR_STORED
     *
     * @param versionId 版本ID（knowledge_document_version.version_id）
     */
    void activateVersion(Long versionId);

    /**
     * 让指定版本失效：
     * 1. 清理该版本在 ES 中的向量数据
     * 2. 将该版本下所有分段状态从 VECTOR_STORED 降为 STORED，并清空 embeddingId
     * 3. 将版本记录状态从 VECTOR_STORED 降为 CHUNKED
     *
     * @param versionId 版本ID（knowledge_document_version.version_id）
     */
    void deactivateVersion(Long versionId);
}
