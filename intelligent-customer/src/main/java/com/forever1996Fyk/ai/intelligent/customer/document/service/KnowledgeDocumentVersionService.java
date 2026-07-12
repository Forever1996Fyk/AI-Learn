package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentVersionEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 文档版本表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
public interface KnowledgeDocumentVersionService extends IService<KnowledgeDocumentVersionEntity> {

    /**
     * 根据文档id获取文档版本列表
     *
     * @param docId 文档id
     * @return 文档版本列表
     */
    List<KnowledgeDocumentVersionEntity> listByDocId(Long docId);

    /**
     * 获取文档最新版本
     *
     * @param docId 文档id
     * @return 文档最新版本
     */
    String getLatestVersion(Long docId);

    /**
     * 根据内容hash判断文档版本是否存在
     *
     * @param contentHash 内容hash
     * @return 文档版本是否存在
     */
    boolean existsByContentHash(String contentHash);

    /**
     * 按 docId 物理删除该文档的所有版本记录
     *
     * @param docId 文档ID
     */
    void physicalDeleteByDocId(Long docId);

    /**
     * 按 docIds 物理删除该文档的所有版本记录
     *
     * @param docIds 文档ID列表
     */
    void physicalDeleteByDocIds(List<Long> docIds);
}
