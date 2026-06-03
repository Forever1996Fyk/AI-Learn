package com.forever1996Fyk.ai.intelligent.customer.document.repository.bean;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
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
    private KnowledgeBaseType knowledgeBaseType;

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
}
