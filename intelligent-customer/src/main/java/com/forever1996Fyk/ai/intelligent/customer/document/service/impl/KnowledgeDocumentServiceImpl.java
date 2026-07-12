package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.SegmentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentVersionEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper.KnowledgeDocumentMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentCleanupService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentVersionService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * 知识文档表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-02
 */
@Slf4j
@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocumentEntity> implements KnowledgeDocumentService {
    @Autowired
    private VectorStoreService vectorStoreService;
    @Autowired
    private DocumentCleanupService documentCleanupService;
    @Autowired
    private ExcelProcessServiceImpl excelProcessServiceImpl;
    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;
    @Autowired
    private KnowledgeDocumentVersionService knowledgeDocumentVersionService;

    /**
     * 删除文档，并级联物理删除该文档下的所有分段和版本，同时按 docId 清除向量存储中的数据
     *
     * @param docId 文档ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocumentWithSegments(Long docId) {
        // 按 metadata 中的 docId 删除该文档所有向量
        deleteVectorsByDocId(docId);

        // 物理删除该文档下的所有分段
        knowledgeSegmentService.physicalDeleteByDocId(docId);

        // 删除该文档对应的 DATA_QUERY 动态物理表
        dropDataQueryTableIfExists(docId);

        // 物理删除该文档的所有版本记录
        knowledgeDocumentVersionService.physicalDeleteByDocId(docId);

        // 物理删除文档本身
        return baseMapper.physicalDeleteByDocId(docId) > 0;
    }

    @Override
    public boolean advanceDocumentAndVersionStatus(Long docId, Long versionId, DocumentStatus targetStatus) {
        Assert.notNull(docId, "文档ID不能为空");
        Assert.notNull(versionId, "版本ID不能为空");
        Assert.notNull(targetStatus, "目标状态不能为空");

        KnowledgeDocumentEntity document = this.getById(docId);
        Assert.notNull(document, "文档不存在: docId=" + docId);

        KnowledgeDocumentVersionEntity version = knowledgeDocumentVersionService.getById(versionId);
        Assert.notNull(version, "版本记录不存在: versionId=" + versionId);
        Assert.isTrue(docId.equals(version.getDocId()), "版本不属于该文档");

        boolean updated = false;

        if (shouldAdvanceStatus(document.getStatus(), targetStatus)) {
            document.setStatus(targetStatus);
            boolean docResult = this.updateById(document);
            Assert.isTrue(docResult, "文档状态更新失败: docId=" + docId);
            updated = true;
            log.info("文档状态已推进, docId={}, status={}", docId, targetStatus);
        } else {
            log.info("文档状态无需推进, docId={}, currentStatus={}, targetStatus={}",
                    docId, document.getStatus(), targetStatus);
        }

        if (shouldAdvanceStatus(version.getStatus(), targetStatus)) {
            version.setStatus(targetStatus);
            boolean versionResult = knowledgeDocumentVersionService.updateById(version);
            Assert.isTrue(versionResult, "版本状态更新失败: versionId=" + versionId);
            updated = true;
            log.info("版本状态已推进, versionId={}, status={}", versionId, targetStatus);
        } else {
            log.info("版本状态无需推进, versionId={}, currentStatus={}, targetStatus={}",
                    versionId, version.getStatus(), targetStatus);
        }
        return updated;
    }

    /**
     * 让指定版本生效（重新向量化）：
     * 1. 校验版本状态必须为 CHUNKED
     * 2. 对该版本下所有 STORED 且未向量化的分段分批 embed 并写入 ES
     * 3. 更新分段状态为 VECTOR_STORED
     * 4. 将版本记录状态从 CHUNKED 升为 VECTOR_STORED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateVersion(Long versionId) {
        KnowledgeDocumentVersionEntity documentVersion = knowledgeDocumentVersionService.getById(versionId);
        Assert.notNull(documentVersion, "版本记录不存在: versionId=" + versionId);

        if (documentVersion.getStatus() == DocumentStatus.VECTOR_STORED) {
            return;
        }

        Assert.isTrue(DocumentStatus.CHUNKED == documentVersion.getStatus(),
                "版本状态不是 CHUNKED，无法执行生效操作，当前状态: " + documentVersion.getStatus());

        Long docId = documentVersion.getDocId();
        log.info("开始让版本生效（重新向量化）, docId={}, versionId={}", docId, versionId);

        // 分页扫描 STORED 且未向量化的分段（skipEmbedding=0）
        LambdaQueryWrapper<KnowledgeSegmentEntity> queryWrapper = Wrappers.<KnowledgeSegmentEntity>lambdaQuery()
                .eq(KnowledgeSegmentEntity::getDocumentId, docId)
                .eq(KnowledgeSegmentEntity::getDocumentVersion, versionId)
                .eq(KnowledgeSegmentEntity::getStatus, SegmentStatus.STORED)
                .eq(KnowledgeSegmentEntity::getSkipEmbedding, 0)
                .isNull(KnowledgeSegmentEntity::getEmbeddingId);

        Page<KnowledgeSegmentEntity> page = knowledgeSegmentService.page(new Page<>(1, 100), queryWrapper);
        while (!page.getRecords().isEmpty()) {
            List<KnowledgeSegmentEntity> batch = page.getRecords();
            List<String> embeddingIds = vectorStoreService.embedAndStore(batch);

            for (int i = 0; i < batch.size(); i++) {
                KnowledgeSegmentEntity seg = batch.get(i);
                seg.setEmbeddingId(embeddingIds.get(i));
                seg.setStatus(SegmentStatus.VECTOR_STORED);
                boolean updateResult = knowledgeSegmentService.updateById(seg);
                Assert.isTrue(updateResult, "分段更新失败: " + seg.getId());
            }

            page = knowledgeSegmentService.page(new Page<>(page.getCurrent(), 100), queryWrapper);
        }

        // 将版本记录状态升为 VECTOR_STORED
        documentVersion.setStatus(DocumentStatus.VECTOR_STORED);
        boolean result = knowledgeDocumentVersionService.updateById(documentVersion);
        Assert.isTrue(result, "版本记录更新失败: " + versionId);
        log.info("版本生效完成, versionId={}", versionId);
    }

    @Override
    public void deactivateVersion(Long versionId) {
        KnowledgeDocumentVersionEntity documentVersion = knowledgeDocumentVersionService.getById(versionId);
        Assert.notNull(documentVersion, "版本记录不存在: versionId=" + versionId);
        if (documentVersion.getStatus() == DocumentStatus.CHUNKED) {
            return;
        }

        Assert.isTrue(DocumentStatus.VECTOR_STORED == documentVersion.getStatus(),
                "版本状态不是 VECTOR_STORED，无法执行失效操作，当前状态: " + documentVersion.getStatus());

        Long docId = documentVersion.getDocId();
        log.info("开始让版本失效, docId={}, versionId={}", docId, versionId);

        // 1. 按 docId + versionId 清理 ES 向量
        documentCleanupService.cleanupOldVersionData(docId, versionId);

        // 2. 将该版本下所有分段状态从 VECTOR_STORED 降为 STORED，并清空 embeddingId
        LambdaUpdateWrapper<KnowledgeSegmentEntity> segUpdate = Wrappers.<KnowledgeSegmentEntity>lambdaUpdate()
                .set(KnowledgeSegmentEntity::getStatus, SegmentStatus.STORED)
                .set(KnowledgeSegmentEntity::getEmbeddingId, null)
                .eq(KnowledgeSegmentEntity::getDocumentId, docId)
                .eq(KnowledgeSegmentEntity::getDocumentVersion, versionId)
                .eq(KnowledgeSegmentEntity::getStatus, SegmentStatus.VECTOR_STORED);
        boolean affected = knowledgeSegmentService.update(null, segUpdate);
        log.info("降级分段状态完成, versionId={}, affected={}", versionId, affected);

        // 3. 将版本记录状态从 VECTOR_STORED 降为 CHUNKED
        documentVersion.setStatus(DocumentStatus.CHUNKED);
        boolean versionUpdateResult = knowledgeDocumentVersionService.updateById(documentVersion);
        Assert.isTrue(versionUpdateResult, "文档版本状态更新失败");
        log.info("版本失效完成, versionId={}", versionId);
    }

    @Override
    public boolean removeDocumentsWithSegments(List<Long> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        // 按 metadata 中的 docId 批量删除所有向量
        deleteVectorsByDocIds(docIds);

        // 物理删除这些文档下的所有分段
        knowledgeSegmentService.physicalDeleteByDocIds(docIds);

        // 删除这些文档对应的 DATA_QUERY 动态物理表
        for (Long docId : docIds) {
            dropDataQueryTableIfExists(docId);
        }

        // 物理删除这些文档的所有版本记录
        knowledgeDocumentVersionService.physicalDeleteByDocIds(docIds);

        // 物理删除文档本身
        return baseMapper.physicalDeleteByDocIds(docIds) > 0;
    }

    /**
     * 按 metadata 中的 docId 批量删除向量
     */
    private void deleteVectorsByDocIds(List<Long> docIds) {
        vectorStoreService.removeByDocIds(docIds);
    }

    /**
     * 判断状态是否需要推进。
     * 当前状态为空或按枚举声明顺序早于目标状态时，才允许推进。
     */
    private boolean shouldAdvanceStatus(DocumentStatus current, DocumentStatus target) {
        if (current == null) {
            return true;
        }
        return current.ordinal() < target.ordinal();
    }


    /**
     * 如果文档是 DATA_QUERY 类型且配置了表名，则删除对应的动态物理表及元数据
     */
    private void dropDataQueryTableIfExists(Long docId) {
        KnowledgeDocumentEntity document = this.getById(docId);
        if (document == null || document.getKnowledgeBaseType() != KnowledgeBaseType.DATA_QUERY || !StringUtils.hasText(document.getTableName())) {
            return;
        }
        String physicalTableName = excelProcessServiceImpl.generatePhysicalTableName(document.getTableName());
        excelProcessServiceImpl.dropTable(physicalTableName);
    }

    /**
     * 按 metadata 中的 docId 删除该文档所有向量
     */
    private void deleteVectorsByDocId(Long docId) {
        vectorStoreService.removeByDocId(docId);
    }
}
