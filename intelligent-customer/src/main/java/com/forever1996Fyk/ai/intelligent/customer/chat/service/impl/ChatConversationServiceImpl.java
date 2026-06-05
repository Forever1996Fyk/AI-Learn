package com.forever1996Fyk.ai.intelligent.customer.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatConversationStatus;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatConversationEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.mapper.ChatConversationMapper;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * AI对话会话表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
@Service
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversationEntity> implements ChatConversationService {

    @Override
    public List<ChatConversationEntity> getConversationsByUserId(String userId) {
        return this.list(new LambdaQueryWrapper<ChatConversationEntity>()
                .eq(ChatConversationEntity::getUserId, userId)
                .ne(ChatConversationEntity::getStatus, "deleted")
                .orderByDesc(ChatConversationEntity::getUpdatedAt));
    }

    @Override
    public ChatConversationEntity getByConversationId(String conversationId) {
        return this.getOne(new LambdaQueryWrapper<ChatConversationEntity>()
                .eq(ChatConversationEntity::getConversationId, conversationId)
                .ne(ChatConversationEntity::getStatus, "deleted"));
    }

    @Override
    public String createConversation(String userId, String title) {
        String conversationId = UUID.randomUUID().toString().replace("-", "") + userId;

        ChatConversationEntity conversation = new ChatConversationEntity();
        conversation.setConversationId(conversationId);
        conversation.setUserId(userId);
        // 这里标题分两步：
        // 1. 同步：先用 content 前 20 个字符作为临时标题，快速建会话
        // 2. 异步：通过线程调用 LLM 生成摘要标题，完成后回写到数据库
        // 参考 ChatController#send
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setStatus(ChatConversationStatus.ACTIVE);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        this.save(conversation);
        return conversationId;
    }

    @Override
    public boolean archiveConversation(String conversationId) {
        return this.update(new LambdaUpdateWrapper<ChatConversationEntity>()
                .eq(ChatConversationEntity::getConversationId, conversationId)
                .set(ChatConversationEntity::getStatus, "archived")
                .set(ChatConversationEntity::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public boolean deleteConversation(String conversationId) {
        return this.update(new LambdaUpdateWrapper<ChatConversationEntity>()
                .eq(ChatConversationEntity::getConversationId, conversationId)
                .set(ChatConversationEntity::getStatus, "deleted")
                .set(ChatConversationEntity::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public boolean updateTitle(String conversationId, String title) {
        return this.update(new LambdaUpdateWrapper<ChatConversationEntity>()
                .eq(ChatConversationEntity::getConversationId, conversationId)
                .set(ChatConversationEntity::getTitle, title)
                .set(ChatConversationEntity::getUpdatedAt, LocalDateTime.now()));
    }
}
