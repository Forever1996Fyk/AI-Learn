package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentVersionEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper.KnowledgeDocumentVersionMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forever1996Fyk.ai.intelligent.customer.document.util.VersionUtil;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * 文档版本表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
@Service
public class KnowledgeDocumentVersionServiceImpl extends ServiceImpl<KnowledgeDocumentVersionMapper, KnowledgeDocumentVersionEntity> implements KnowledgeDocumentVersionService {
    /**
     * 语义化版本比较器（按 major.minor.patch 数值比较）
     */
    private static final Comparator<KnowledgeDocumentVersionEntity> VERSION_COMPARATOR =
            Comparator.comparing(KnowledgeDocumentVersionEntity::getVersion, VersionUtil::compareVersions);

    @Override
    public List<KnowledgeDocumentVersionEntity> listByDocId(Long docId) {
        List<KnowledgeDocumentVersionEntity> versions = list(new QueryWrapper<KnowledgeDocumentVersionEntity>()
                .eq("doc_id", docId));
        // 在 Java 层按语义版本降序排序
        versions.sort(VERSION_COMPARATOR.reversed());
        return versions;
    }

    @Override
    public String getLatestVersion(Long docId) {
        List<KnowledgeDocumentVersionEntity> versions = listByDocId(docId);
        if (versions.isEmpty()) {
            return null;
        }
        return versions.getFirst().getVersion();
    }

    @Override
    public boolean existsByContentHash(String contentHash) {
        return count(new QueryWrapper<KnowledgeDocumentVersionEntity>()
                .eq("content_hash", contentHash)) > 0;
    }

    @Override
    public void physicalDeleteByDocId(Long docId) {
        baseMapper.physicalDeleteByDocId(docId);
    }

    @Override
    public void physicalDeleteByDocIds(List<Long> docIds) {
        baseMapper.physicalDeleteByDocIds(docIds);
    }
}
