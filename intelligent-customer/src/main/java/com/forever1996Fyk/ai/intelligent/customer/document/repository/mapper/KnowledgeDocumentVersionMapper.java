package com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentVersionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 文档版本表 Mapper 接口
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
public interface KnowledgeDocumentVersionMapper extends BaseMapper<KnowledgeDocumentVersionEntity> {

    /** 按文档ID物理删除该文档的所有版本记录 */
    @Delete("DELETE FROM knowledge_document_version WHERE doc_id = #{docId}")
    int physicalDeleteByDocId(@Param("docId") Long docId);

    /** 按文档ID列表批量物理删除所有版本记录 */
    @Delete("<script>DELETE FROM knowledge_document_version WHERE doc_id IN " +
            "<foreach item='docId' collection='docIds' open='(' separator=',' close=')'>#{docId}</foreach></script>")
    void physicalDeleteByDocIds(List<Long> docIds);
}
