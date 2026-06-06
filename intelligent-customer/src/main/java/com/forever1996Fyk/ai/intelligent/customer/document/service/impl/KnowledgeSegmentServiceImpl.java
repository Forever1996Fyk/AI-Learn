package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper.KnowledgeSegmentMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 知识片段表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
@Service
public class KnowledgeSegmentServiceImpl extends ServiceImpl<KnowledgeSegmentMapper, KnowledgeSegmentEntity> implements KnowledgeSegmentService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public String getTextByChunkId(String chunkId) {
        String text = stringRedisTemplate.opsForValue().get(chunkId);
        if (StringUtils.hasText(text)) {
            if (!"NONE".equals(text)) {
                return text;
            } else {
                return null;
            }
        }
        KnowledgeSegmentEntity knowledgeSegment = this.getOne(
                new QueryWrapper<KnowledgeSegmentEntity>().lambda()
                        .eq(KnowledgeSegmentEntity::getChunkId, chunkId)
        );
        if (knowledgeSegment != null) {
            stringRedisTemplate.opsForValue().set(chunkId, knowledgeSegment.getText(), 30, TimeUnit.SECONDS);
            return knowledgeSegment.getText();
        } else {
            // 缓存空值，避免缓存击穿，重复查询数据库
            stringRedisTemplate.opsForValue().set(chunkId, "NONE");
        }

        return null;
    }
}
