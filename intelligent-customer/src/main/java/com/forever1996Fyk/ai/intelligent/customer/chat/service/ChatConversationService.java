package com.forever1996Fyk.ai.intelligent.customer.chat.service;

import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatConversationEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * AI对话会话表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
public interface ChatConversationService extends IService<ChatConversationEntity> {

    /**
     * 根据用户ID获取会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatConversationEntity> getConversationsByUserId(String userId);

    /**
     * 根据会话ID获取会话
     *
     * @param conversationId 会话ID
     * @return 会话
     */
    ChatConversationEntity getByConversationId(String conversationId);

    /**
     * 创建会话
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 会话ID
     */
    String createConversation(String userId, String title);

    /**
     * 归档会话
     *
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean archiveConversation(String conversationId);

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean deleteConversation(String conversationId);

    /**
     * 更新会话标题
     *
     * @param finalConversationId 会话ID
     * @param aiTitle              AI生成的标题
     */
    boolean updateTitle(String finalConversationId, String aiTitle);
}
