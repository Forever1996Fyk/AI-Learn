package com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

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

    /** 按文档ID列表批量物理删除所有分段 */
    @Delete("<script>DELETE FROM knowledge_segment WHERE document_id IN " +
            "<foreach item='docId' collection='docIds' open='(' separator=',' close=')'>#{docId}</foreach></script>")
    void physicalDeleteByDocIds(List<Long> docIds);
}
