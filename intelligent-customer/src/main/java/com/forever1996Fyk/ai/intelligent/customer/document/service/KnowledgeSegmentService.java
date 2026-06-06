package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 知识片段表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
public interface KnowledgeSegmentService extends IService<KnowledgeSegmentEntity> {

    /**
     * 根据chunkId获取文本
     *
     * @param parentChunkId chunkId
     * @return 文本
     */
    String getTextByChunkId(String parentChunkId);
}
