package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper.KnowledgeSegmentMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.VectorStoreService;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 知识片段表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
@Slf4j
@Service
public class KnowledgeSegmentServiceImpl extends ServiceImpl<KnowledgeSegmentMapper, KnowledgeSegmentEntity> implements KnowledgeSegmentService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private VectorStoreService vectorStoreService;
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

    @Override
    public void physicalDeleteByDocId(Long docId) {
        baseMapper.physicalDeleteByDocId(docId);
    }

    @Override
    public void physicalDeleteByDocIds(List<Long> docIds) {
        baseMapper.physicalDeleteByDocIds(docIds);
    }

    @Override
    public boolean updateById(KnowledgeSegmentEntity entity, boolean updateVectorStore) {
        KnowledgeSegmentEntity oldSegment = super.getById(entity.getId());
        if (oldSegment == null) {
            return super.updateById(entity);
        }

        // 判断分块文本是否变动
        boolean textChanged = entity.getText() != null && !entity.getText().equals(oldSegment.getText());

        if (updateVectorStore) {
            boolean hasEmbedding = oldSegment.getEmbeddingId() != null;
            boolean skipEmbedding = oldSegment.getSkipEmbedding() != null && oldSegment.getSkipEmbedding() == 1;

            if (textChanged && hasEmbedding && !skipEmbedding) {
                // 1. 删除旧向量
                vectorStoreService.remove(oldSegment.getEmbeddingId());

                // 2. 生成新向量并写入 ES
                try {
                    // 若更新请求未携带 metadata，复用旧分段的 metadata，避免检索过滤信息丢失
                    if (entity.getMetadata() == null && oldSegment.getMetadata() != null) {
                        entity.setMetadata(oldSegment.getMetadata());
                    }
                    String newEmbeddingId = vectorStoreService.embedAndStore(entity);
                    entity.setEmbeddingId(newEmbeddingId);
                    log.info("更新向量成功, segmentId: {}, oldEmbeddingId: {}, newEmbeddingId: {}",
                            entity.getId(), oldSegment.getEmbeddingId(), newEmbeddingId);
                } catch (Exception e) {
                    // 旧向量已删除，新向量写入失败，清空 embeddingId 保持 DB 与 ES 一致
                    entity.setEmbeddingId(null);
                    log.error("更新向量失败, segmentId: {}, error: {}", entity.getId(), e.getMessage(), e);
                }
            }
        }

        // 文本变更时，同步更新父分段内容
        if (textChanged) {
            syncParentSegmentText(oldSegment, entity.getText());
        }
        return false;
    }

    /**
     * 当子分段文本变更时，同步更新对应父分段的文本内容。
     * <p>
     * 父分段存储完整文本（skipEmbedding=1），子分段是其中的子串。
     * 修改子分段时需将父分段中对应的旧文本替换为新文本，并清除 Redis 缓存，保证检索时获取最新内容。
     *
     * @param oldSegment 修改前的子分段（包含旧文本和 metadata）
     * @param newText   修改后的新文本
     */
    private void syncParentSegmentText(KnowledgeSegmentEntity oldSegment, String newText) {
        Map<String, String> metadataMap = oldSegment.getMetadataMap();
        if (metadataMap == null) {
            return;
        }
        String parentChunkId = metadataMap.get(MetadataKeyConstant.PARENT_CHUNK_ID);
        if (parentChunkId == null) {
            return;
        }

        // 查找父分段
        QueryWrapper<KnowledgeSegmentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chunk_id", parentChunkId);
        KnowledgeSegmentEntity parentSegment = super.getOne(queryWrapper);
        if (parentSegment == null) {
            log.warn("子分段修改后同步父分段失败：未找到父分段, parentChunkId: {}", parentChunkId);
            return;
        }

        // 将父分段文本中的旧子分段文本替换为新文本
        String oldText = oldSegment.getText();
        String parentText = parentSegment.getText();
        String updatedParentText = parentText.replaceFirst(Pattern.quote(oldText), Matcher.quoteReplacement(newText));

        if (!updatedParentText.equals(parentText)) {
            parentSegment.setText(updatedParentText);
            super.updateById(parentSegment);
            log.info("子分段修改已同步更新父分段, parentChunkId: {}, parentSegmentId: {}", parentChunkId, parentSegment.getId());

            // 清除父分段的 Redis 缓存
            stringRedisTemplate.delete(parentChunkId);
        } else {
            log.warn("子分段修改后父分段文本未匹配到旧文本，跳过同步, parentChunkId: {}, segmentId: {}", parentChunkId, oldSegment.getId());
        }
    }
}
