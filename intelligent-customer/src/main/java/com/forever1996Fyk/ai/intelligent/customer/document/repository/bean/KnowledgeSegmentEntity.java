package com.forever1996Fyk.ai.intelligent.customer.document.repository.bean;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.SegmentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 知识片段表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
@Getter
@Setter
@ToString
@TableName("knowledge_segment")
public class KnowledgeSegmentEntity {

    /**
     * 片段ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 分片ID
     */
    private String chunkId;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 所属文档ID
     */
    private Long documentId;

    /**
     * 顺序
     */
    private Integer chunkOrder;

    /**
     * 嵌入ID
     */
    private String embeddingId;

    /**
     * 状态：STORED, VECTOR_STORED
     */
    private SegmentStatus status;

    /**
     * 是否跳过嵌入生成
     */
    private Integer skipEmbedding;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 修改时间
     */
    private Date updatedAt;

    /**
     * 乐观锁版本号
     */
    private Integer lockVersion;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Integer deleted;

    @JsonIgnore
    public Map<String, String> getMetadataMap() {
        return metadata == null ? null : JSON.parseObject(metadata, Map.class);
    }
}
