package com.forever1996Fyk.ai.intelligent.customer.document.service;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/10 00:08
 **/
public interface DocumentCleanupService {

    /**
     * 清理指定文档的旧版本分段和向量数据
     * 仅删除 document_version != currentVersionId 的分段和向量，保留当前版本数据
     *
     * @param docId     文档ID
     * @param versionId 当前激活版本ID
     */
    boolean cleanupOldVersionData(Long docId, Long versionId);
}
