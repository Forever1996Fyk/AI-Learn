package com.forever1996Fyk.ai.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forever1996Fyk.ai.agent.domain.SaveQuestionRequest;
import com.forever1996Fyk.ai.agent.domain.UpdateAnswerRequest;
import com.forever1996Fyk.ai.agent.repository.bean.AiSessionEntity;
import com.forever1996Fyk.ai.agent.repository.mapper.AiSessionMapper;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 存储智能体与用户的对话历史，支持会话隔离和记忆功能 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Service
public class AiSessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSessionEntity> implements AiSessionService {

    @Override
    public List<AiSessionEntity> listRecentByConversationId(String conversationId, int maxRecords) {
        return baseMapper.selectList(
                new QueryWrapper<AiSessionEntity>().lambda()
                        .orderByDesc(AiSessionEntity::getCreateTime)
                        .eq(AiSessionEntity::getSessionId, conversationId)
                        .last("LIMIT " + maxRecords)
        );
    }

    @Override
    public AiSessionEntity saveQuestion(SaveQuestionRequest request) {
        AiSessionEntity aiSession = new AiSessionEntity();
        aiSession.setSessionId(request.getSessionId());
        aiSession.setQuestion(request.getQuestion());
        aiSession.setFileid(request.getFileid());
        aiSession.setTools(request.getTools());
        aiSession.setFirstResponseTime(request.getFirstResponseTime());
        aiSession.setCreateTime(LocalDateTime.now());
        aiSession.setUpdateTime(LocalDateTime.now());
        this.save(aiSession);
        return aiSession;
    }

    @Override
    public boolean updateAnswer(UpdateAnswerRequest request) {
        AiSessionEntity session = this.getById(request.getId());
        if (session != null) {
            session.setAnswer(request.getAnswer());
            session.setUpdateTime(LocalDateTime.now());
            if (request.getThinking() != null) {
                session.setThinking(request.getThinking());
            }
            if (request.getTools() != null) {
                session.setTools(request.getTools());
            }
            if (request.getReference() != null) {
                session.setReference(request.getReference());
            }
            if (request.getFirstResponseTime() != null) {
                session.setFirstResponseTime(request.getFirstResponseTime());
            }
            if (request.getTotalResponseTime() != null) {
                session.setTotalResponseTime(request.getTotalResponseTime());
            }
            if(request.getRecommend() != null){
                session.setRecommend(request.getRecommend());
            }
            return this.updateById(session);
        }
        return false;
    }
}
