package com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 知识文档表 Mapper 接口
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {

    /** 按主键物理删除单个文档 */
    @Delete("DELETE FROM knowledge_document WHERE doc_id = #{docId}")
    int physicalDeleteByDocId(@Param("docId") Long docId);

}
