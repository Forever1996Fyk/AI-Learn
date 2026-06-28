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

    /**
     * 更新需求信息
     *
     * @param id          实例ID
     * @param response    响应
     * @param targetStatus 目标状态
     */
    boolean updateRequirement(Long id, String response, PptInstStatus targetStatus);

    /**
     * 更新搜索结果
     *
     * @param id 实例ID
     * @return 搜索结果
     */
    boolean updateSearchInfo(Long id, String searchResult, PptInstStatus targetStatus);
    /**
     * 更新模板编码
     *
     * @param id           实例ID
     * @param templateCode 模板编码
     * @param status       状态
     * @return 是否更新成功
     */
    boolean updateTemplateCode(Long id, String templateCode, PptInstStatus status);

    /**
     * 更新大纲
     *
     * @param id     实例ID
     * @param outline 大纲
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateOutline(Long id, String outline, PptInstStatus status);

    /**
     * 更新PPT Schema
     *
     * @param id        实例ID
     * @param pptSchema PPT Schema JSON
     * @param status    状态
     * @return 是否更新成功
     */
    boolean updatePptSchema(Long id, String pptSchema, PptInstStatus status);

    /**
     * 更新文件URL
     *
     * @param id      实例ID
     * @param fileUrl 文件URL
     * @param status  状态
     * @return 是否更新成功
     */
    boolean updateFileUrl(Long id, String fileUrl, PptInstStatus status);
}
