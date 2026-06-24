package com.forever1996Fyk.ai.agent.service;

import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * AI PPT生成实例表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
public interface AiPptInstService extends IService<AiPptInstEntity> {

    /**
     * 创建新的PPT实例
     *
     * @param conversationId 会话ID
     * @param query          用户原始需求
     * @return PPT实例
     */
    AiPptInstEntity createInst(String conversationId, String query);

    /**
     * 更新错误信息
     *
     * @param id       实例ID
     * @param errorMsg 错误信息
     * @param status   状态
     * @return 是否更新成功
     */
    boolean updateError(Long id, String errorMsg, PptInstStatus status);

    /**
     * 获取最新的PPT实例
     *
     * @param conversationId 会话ID
     * @return 最新的PPT实例
     */
    AiPptInstEntity getLatestInst(String conversationId);
}
