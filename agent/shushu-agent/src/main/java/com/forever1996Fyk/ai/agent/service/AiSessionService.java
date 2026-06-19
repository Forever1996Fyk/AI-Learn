package com.forever1996Fyk.ai.agent.service;

import com.forever1996Fyk.ai.agent.domain.SaveQuestionRequest;
import com.forever1996Fyk.ai.agent.domain.UpdateAnswerRequest;
import com.forever1996Fyk.ai.agent.repository.bean.AiSessionEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 存储智能体与用户的对话历史，支持会话隔离和记忆功能 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
public interface AiSessionService extends IService<AiSessionEntity> {

    /**
     * 获取最近的会话记录
     *
     * @param conversationId 会话ID
     * @param maxRecords    最大记录数
     * @return 最近的会话记录
     */
    List<AiSessionEntity> listRecentByConversationId(String conversationId, int maxRecords);

    /**
     * 保存用户问题
     *
     * @param request 保存用户问题的请求
     * @return 保存后的会话记录
     */
    AiSessionEntity saveQuestion(SaveQuestionRequest request);

    /**
     * 更新AI回复
     *
     * @param request 更新AI回复的请求
     * @return 是否更新成功
     */
    boolean updateAnswer(UpdateAnswerRequest request);
}
