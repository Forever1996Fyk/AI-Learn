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
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentUploadParam;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import com.forever1996Fyk.ai.intelligent.customer.document.util.DocumentPermissionUtils;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.RoleEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

//    /**
//     * 上传用户
//     */
//    private String uploadUser;

//    /**
//     * 文档URL
//     */
//    private String docUrl;
//
//    /**
//     * 转换后的文档URL
//     */
//    private String convertedDocUrl;

//    /**
//     * 文档失效日期
//     */
//    private Date expireDate;

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
    private KnowledgeBaseType knowledgeBaseType;

    /**
     * 扩展字段，保存JSON字符串
     */
    private String extension;


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

    /**
     * 当前激活版本ID，指向 knowledge_document_version.version_id
     */
    private Long currentVersionId;

    @JsonIgnore
    public Boolean isOverride() {
        if (extension != null && !extension.isEmpty()) {
            return (Boolean) JSON.parseObject(extension, Map.class).get("isOverride");
        }
        return false;
    }

    @JsonIgnore
    public String getTableName() {
        if (extension != null && !extension.isEmpty()) {
            return (String) JSON.parseObject(extension, Map.class).get("tableName");
        }
        return null;
    }


    @JsonIgnore
    public void setTableName(String tableName) {
        Map<String, Serializable> extensionMap;
        if (extension == null) {
            extensionMap = new HashMap<String, Serializable>();
        } else {
            extensionMap = JSON.parseObject(extension, Map.class);
        }
        extensionMap.put("tableName", tableName);
        this.extension = JSON.toJSONString(extensionMap);
    }

    public KnowledgeDocumentEntity create(DocumentUploadParam documentUploadParam) {
        this.setDocTitle(documentUploadParam.title());
        this.setStatus(DocumentStatus.UPLOADED);
        this.setDescription(documentUploadParam.description());
        this.setKnowledgeBaseType(KnowledgeBaseType.valueOf(documentUploadParam.knowledgeBaseType()));
        this.setTableName(documentUploadParam.tableName());
        this.setAccessibleBy(DocumentPermissionUtils.getDocumentPermission(RoleEnum.valueOf(documentUploadParam.accessibleBy())));
        return this;
    }
}
