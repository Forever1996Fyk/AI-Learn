package com.forever1996Fyk.ai.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptTemplateEntity;
import com.forever1996Fyk.ai.agent.repository.mapper.AiPptTemplateMapper;
import com.forever1996Fyk.ai.agent.service.AiPptTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * AI PPT模板表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Service
public class AiPptTemplateServiceImpl extends ServiceImpl<AiPptTemplateMapper, AiPptTemplateEntity> implements AiPptTemplateService {

    @Override
    public List<AiPptTemplateEntity> getAllTemplates() {
        LambdaQueryWrapper<AiPptTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AiPptTemplateEntity::getCreateTime);
        return list(wrapper);
    }

    @Override
    public AiPptTemplateEntity getByCode(String templateCode) {
        LambdaQueryWrapper<AiPptTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPptTemplateEntity::getTemplateCode, templateCode);
        return getOne(wrapper);
    }
}
