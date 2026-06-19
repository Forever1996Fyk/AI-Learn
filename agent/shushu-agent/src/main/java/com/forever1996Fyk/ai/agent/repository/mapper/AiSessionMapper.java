package com.forever1996Fyk.ai.agent.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forever1996Fyk.ai.agent.repository.bean.AiSessionEntity;

/**
 * <p>
 * 存储智能体与用户的对话历史，支持会话隔离和记忆功能 Mapper 接口
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
public interface AiSessionMapper extends BaseMapper<AiSessionEntity> {

}
