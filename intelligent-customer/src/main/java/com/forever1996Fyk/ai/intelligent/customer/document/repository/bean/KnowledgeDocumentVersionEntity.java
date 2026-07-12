package com.forever1996Fyk.ai.intelligent.customer.document.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * <p>
 * 文档版本表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
@Getter
@Setter
@ToString
@TableName("knowledge_document_version")
public class KnowledgeDocumentVersionEntity {

    /**
     * 版本ID
     */
    @TableId(value = "version_id", type = IdType.AUTO)
    private Long versionId;

    /**
     * 关联文档ID（knowledge_document.doc_id）
     */
    private Long docId;

    /**
     * 版本号（语义化版本，如 1.0.0）
     */
    @Version
    private String version;

    /**
     * 该版本文档URL（MinIO原始文件）
     */
    private String docUrl;

    /**
     * 该版本转换后的文档URL
     */
    private String convertedDocUrl;

    /**
     * 该版本文档内容哈希值（SHA-256）
     */
    private String contentHash;

    /**
     * 版本状态：UPLOADED, CONVERTING, CONVERTED, CHUNKED, VECTOR_STORED, STORED
     */
    private DocumentStatus status;

    /**
     * 该版本上传用户
     */
    private String uploadUser;

    /**
     * 版本变更说明
     */
    private String changelog;

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
}
