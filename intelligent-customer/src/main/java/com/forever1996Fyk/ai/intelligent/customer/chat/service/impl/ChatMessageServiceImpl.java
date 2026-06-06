package com.forever1996Fyk.ai.intelligent.customer.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatMessageType;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatMessageEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.mapper.ChatMessageMapper;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatMessageService;
import dev.langchain4j.data.message.ChatMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * AI对话消息表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessageEntity> implements ChatMessageService {

    @Override
    public List<ChatMessageEntity> getMessagesByConversationId(String conversationId) {
        return this.list(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getConversationId, conversationId)
                .orderByAsc(ChatMessageEntity::getCreatedAt));
    }

    @Override
    public ChatMessageEntity getByMessageId(String messageId) {
        return this.getOne(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getMessageId, messageId));
    }

    @Override
    public String saveUserMessage(String conversationId, String content) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        ChatMessageEntity message = new ChatMessageEntity();
        message.setMessageId(messageId);
        message.setConversationId(conversationId);
        message.setType(ChatMessageType.USER);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        this.save(message);
        return messageId;
    }

    @Override
    public List<ChatMessageEntity> getRecentMessages(String conversationId, int limit) {
        // 查询最新的 limit+2 条，排除最新的2条（当前轮次刚保存的user消息和空assistant消息）
        Page<ChatMessageEntity> page = this.page(
                new Page<>(1, limit + 2),
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getConversationId, conversationId)
                        .orderByDesc(ChatMessageEntity::getCreatedAt)
        );
        List<ChatMessageEntity> records = page.getRecords();
        // 去掉最新的2条
        if (records.size() > 2) {
            records = records.subList(2, records.size());
        } else {
            return new java.util.ArrayList<>();
        }

        // 返回列表需要反转，使其按时间正序排列
        java.util.Collections.reverse(records);
        return records;
    }

    @Override
    public boolean deleteMessagesByConversationId(String conversationId) {
        return this.remove(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getConversationId, conversationId));
    }

    @Override
    public String saveAssistantMessage(String conversationId) {
        String messageId = UUID.randomUUID().toString().replace("-", "");

        ChatMessageEntity message = new ChatMessageEntity();
        message.setMessageId(messageId);
        message.setConversationId(conversationId);
        message.setType(ChatMessageType.ASSISTANT);
        message.setCreatedAt(LocalDateTime.now());

        this.save(message);
        return messageId;
    }

    @Override
    public void updateTransformContent(String chatMessageId, String newQuery) {
        ChatMessageEntity chatMessageEntity = new ChatMessageEntity();
        chatMessageEntity.setTransformContent(newQuery);
        this.update(chatMessageEntity, new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getMessageId, chatMessageId));
    }
}
