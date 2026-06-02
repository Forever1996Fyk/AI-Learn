package com.forever1996Fyk.ai.intelligent.customer.document.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * <p>
 * 知识文档表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
@Getter
@Setter
@ToString
@TableName("knowledge_document")
public class KnowledgeDocumentEntity {

    /**
     * 文档ID
     */
    @TableId(value = "doc_id", type = IdType.AUTO)
    private Long docId;

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 上传用户
     */
    private String uploadUser;

    /**
     * 文档URL
     */
    private String docUrl;

    /**
     * 转换后的文档URL
     */
    private String convertedDocUrl;

    /**
     * 文档失效日期
     */
    private Date expireDate;

    /**
     * 状态：INIT, UPLOADED, CONVERTING, CONVERTED, CHUNKED, VECTOR_STORED
     */
    private DocumentStatus status;

    /**
     * 可见范围
     */
    private String accessibleBy;

    /**
     * 文档描述
     */
    private String description;

    /**
     * 知识库类型：DOCUMENT_SEARCH, DATA_QUERY
     */
    private String knowledgeBaseType;

    /**
     * 扩展字段，保存JSON字符串
     */
    private String extension;

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
    private Byte deleted;
}
