package com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import org.apache.ibatis.annotations.Delete;

/**
 * <p>
 * 知识片段表 Mapper 接口
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
public interface KnowledgeSegmentMapper extends BaseMapper<KnowledgeSegmentEntity> {

    /** 按文档ID物理删除该文档下所有分段 */
    @Delete("DELETE FROM knowledge_segment WHERE document_id = #{docId}")
    void physicalDeleteByDocId(Long docId);
}
