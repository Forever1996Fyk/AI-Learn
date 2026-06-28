package com.forever1996Fyk.ai.agent.service;

import com.forever1996Fyk.ai.agent.repository.bean.AiPptTemplateEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * AI PPT模板表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
public interface AiPptTemplateService extends IService<AiPptTemplateEntity> {

    /**
     * 获取所有可用模板
     *
     * @return 模板列表
     */
    List<AiPptTemplateEntity> getAllTemplates();

    /**
     * 根据模板编码获取模板
     *
     * @param templateCode 模板编码
     * @return 模板
     */
    AiPptTemplateEntity getByCode(String templateCode);
}
