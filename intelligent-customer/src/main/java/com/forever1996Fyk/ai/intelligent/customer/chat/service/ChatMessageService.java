package com.forever1996Fyk.ai.intelligent.customer.chat.service;

import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatMessageEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * AI对话消息表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
public interface ChatMessageService extends IService<ChatMessageEntity> {

    /**
     * 根据会话ID获取消息列表
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<ChatMessageEntity> getMessagesByConversationId(String conversationId);

    /**
     * 根据消息ID获取消息
     *
     * @param messageId 消息ID
     * @return 消息
     */
    ChatMessageEntity getByMessageId(String messageId);

    /**
     * 保存用户消息
     *
     * @param conversationId 会话ID
     * @param content        消息内容
     * @return 消息ID
     */
    String saveUserMessage(String conversationId, String content);

    /**
     * 获取最近的消息
     *
     * @param conversationId 会话ID
     * @param limit          限制数量
     * @return 消息列表
     */
    List<ChatMessageEntity> getRecentMessages(String conversationId, int limit);

    /**
     * 根据会话ID删除消息
     *
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean deleteMessagesByConversationId(String conversationId);

    /**
     * 保存助手消息
     *
     * @param finalConversationId 最终会话ID
     * @return 消息ID
     */
    String saveAssistantMessage(String finalConversationId);

    /**
     * 更新转换后的内容
     *
     * @param chatMessageId 聊天消息ID
     * @param newQuery      新的查询内容
     */
    void updateTransformContent(String chatMessageId, String newQuery);

    /**
     * 更新RAG参考内容
     *
     * @param chatMessageId       聊天消息ID
     * @param ragReferenceChunks  RAG参考内容块
     */
    void updateRagReferences(String chatMessageId, List<ChatMessageEntity.RagReference> ragReferenceChunks);

    /**
     * 更新内容
     *
     * @param assistantMessageId 助手消息ID
     * @param content            内容
     */
    void updateContent(String assistantMessageId, String content);
}
