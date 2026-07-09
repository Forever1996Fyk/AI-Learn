package com.forever1996Fyk.ai.intelligent.customer.document.repository.bean;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.SegmentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
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
     * 所属文档版本ID（指向 knowledge_document_version.version_id）
     */
    private Long documentVersion;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer lockVersion;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;

    @JsonIgnore
    public Map<String, String> getMetadataMap() {
        return metadata == null ? null : JSON.parseObject(metadata, Map.class);
    }
}
