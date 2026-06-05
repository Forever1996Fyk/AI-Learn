package com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatMessageType;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.RetrievalSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI对话消息表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
@Getter
@Setter
@ToString
@TableName("chat_message")
public class ChatMessageEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 角色：USER/ASSISTANT
     */
    private ChatMessageType type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 改写后的内容
     */
    private String transformContent;

    /**
     * Token数量
     */
    private Integer tokenCount;

    /**
     * 使用的模型名称
     */
    private String modelName;

    /**
     * RAG引用内容JSON数组，
     * 包含document_id、document_title、chunk_id、chunk_content、similarity_score、retrieval_source等字段
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<RagReference> ragReferences;


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
     * 扩展元数据JSON格式
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;


    /**
     * RAG引用内容内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagReference {
        /**
         * 文档ID
         */
        private String documentId;

        /**
         * 文档URL
         */
        private String url;

        /**
         * 文档标题
         */
        private String documentTitle;

        /**
         * 文档块ID
         */
        private String chunkId;

        /**
         * 文档块内容
         */
        private String chunkContent;

        /**
         * 相似度分数
         */
        private Double similarityScore;

        private Double rerankScore;

        /**
         * 检索来源：vector/keyword/hybrid/rerank
         */
        private RetrievalSource retrievalSource;

        /**
         * 扩展元数据
         */
        private Map<String, Object> metadata;

    }
}
