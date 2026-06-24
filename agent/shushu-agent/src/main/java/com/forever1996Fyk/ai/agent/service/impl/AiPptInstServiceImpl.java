package com.forever1996Fyk.ai.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import com.forever1996Fyk.ai.agent.repository.mapper.AiPptInstMapper;
import com.forever1996Fyk.ai.agent.service.AiPptInstService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * AI PPT生成实例表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Slf4j
@Service
public class AiPptInstServiceImpl extends ServiceImpl<AiPptInstMapper, AiPptInstEntity> implements AiPptInstService {

    @Override
    public AiPptInstEntity createInst(String conversationId, String query) {
        AiPptInstEntity inst = AiPptInstEntity.builder()
                .conversationId(conversationId)
                .query(query)
                .status(PptInstStatus.INIT.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        save(inst);
        log.info("创建PPT实例: id={}, conversationId={}", inst.getId(), conversationId);
        return inst;
    }

    @Override
    public boolean updateError(Long id, String errorMsg, PptInstStatus status) {
        AiPptInstEntity inst = new AiPptInstEntity();
        inst.setId(id);
        inst.setErrorMsg(errorMsg);
        inst.setStatus(status.getCode());
        inst.setUpdateTime(LocalDateTime.now());
        return updateById(inst);
    }

    @Override
    public AiPptInstEntity getLatestInst(String conversationId) {
        LambdaQueryWrapper<AiPptInstEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPptInstEntity::getConversationId, conversationId)
                .orderByDesc(AiPptInstEntity::getCreateTime)
                .last("LIMIT 1");
        return getOne(wrapper);
    }
}
