package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentSplitParam;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentUploadParam;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.SegmentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.event.DocumentChunkedEvent;
import com.forever1996Fyk.ai.intelligent.customer.document.factory.FileProcessServiceFactory;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileStorageService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.document.util.FileTypeUtils;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.spltter.DocumentSplitterFactory;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 11:02
 **/
@Slf4j
@Service
public class DocumentProcessServiceImpl implements DocumentProcessService {
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;
    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;
    @Autowired
    private FileProcessServiceFactory fileProcessServiceFactory;

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;
    @Autowired
    private ElasticsearchEmbeddingStore embeddingStore;


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Override
    public KnowledgeDocumentEntity upload(DocumentUploadParam documentUploadParam) throws IOException {
        log.info("start to upload");
        String fileName = documentUploadParam.file().getOriginalFilename();
        try {
            // minio 上传
            String fileUrl = fileStorageService.uploadFile(documentUploadParam.file(), fileName);

            // 构建文档记录
            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setDocTitle(documentUploadParam.title());
            document.setUploadUser(documentUploadParam.uploadUser());
            document.setDocUrl(fileUrl);
            document.setStatus(DocumentStatus.UPLOADED);
            document.setAccessibleBy(documentUploadParam.accessibleBy());
            document.setDescription(documentUploadParam.description());
            document.setKnowledgeBaseType(KnowledgeBaseType.valueOf(documentUploadParam.knowledgeBaseType()));
            document.setTableName(documentUploadParam.tableName());
            boolean result = knowledgeDocumentService.save(document);
            Assert.isTrue(result, "文件上传失败");

            FileProcessService fileProcessService = fileProcessServiceFactory.get(FileTypeUtils.getFileType(fileName, documentUploadParam.file()), document.getKnowledgeBaseType());
            if (fileProcessService != null) {
                fileProcessService.processDocument(document, documentUploadParam.file().getInputStream());
            } else {
                if (document.getKnowledgeBaseType() == KnowledgeBaseType.DOCUMENT_SEARCH) {
                    document.setStatus(DocumentStatus.CONVERTED);
                    document.setConvertedDocUrl(fileUrl);
                    result = knowledgeDocumentService.updateById(document);
                    Assert.isTrue(result, "文件状态更新失败");
                } else {
                    document.setStatus(DocumentStatus.STORED);
                    document.setConvertedDocUrl(fileUrl);
                    result = knowledgeDocumentService.updateById(document);
                    Assert.isTrue(result, "文件状态更新失败");
                }
            }
            return document;
        } catch (Exception e) {
            throw new IOException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int split(KnowledgeDocumentEntity document, DocumentSplitParam documentSplitParam) {
        // 1. 查询文档
        Assert.notNull(document, "文档不存在");
        Assert.notNull(document.getConvertedDocUrl(), "文档未转换完成");

        if (DocumentStatus.CHUNKED == document.getStatus()) {
            //返回已切分的分段数量
            long count = knowledgeSegmentService.count(
                    new QueryWrapper<KnowledgeSegmentEntity>().lambda()
                            .eq(KnowledgeSegmentEntity::getDocumentId, document.getDocId())
                            .eq(KnowledgeSegmentEntity::getSkipEmbedding, 0)
            );
            return Math.toIntExact(count);
        }

        if (document.getStatus() != DocumentStatus.CONVERTED) {
            throw new RuntimeException("文档状态不是CONVERTED，无法切分");
        }

        // 2.从 MinIO下载文件内容
        String convertedDocUrl = document.getConvertedDocUrl();
        String objectName = extractObjectNameFromUrl(convertedDocUrl);
        Assert.notNull(objectName, "无法解析文档 URL");

        List<KnowledgeSegmentEntity> knowledgeSegments = Lists.newArrayList();
        List<TextSegment> segments = Lists.newArrayList();
        try (InputStream inputStream = fileStorageService.downloadFile(objectName)){
            DocumentSplitter splitter = DocumentSplitterFactory.getInstance(documentSplitParam);
            Document doc = Document.from(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            segments = splitter.split(doc);
        } catch (Exception e) {
            throw new RuntimeException("下载文档失败：" + e.getMessage(), e);
        }

        // 4. 转换为 KnowledgeDocumentEntity 并保存
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            KnowledgeSegmentEntity knowledgeSegment = new KnowledgeSegmentEntity();
            knowledgeSegment.setText(segment.text());
            knowledgeSegment.setChunkId(segment.metadata().getString(MetadataKeyConstant.CHUNK_ID));
            Metadata metadata = segment.metadata();
            metadata.put(MetadataKeyConstant.DOC_ID, document.getDocId());
            metadata.put(MetadataKeyConstant.FILE_NAME, document.getDocTitle());
            metadata.put(MetadataKeyConstant.URL, document.getDocUrl());

            //todo metadata统一处理(权限相关、多版本相关）

            knowledgeSegment.setMetadata(JSON.toJSONString(metadata.toMap()));
            knowledgeSegment.setDocumentId(document.getDocId());
            knowledgeSegment.setChunkOrder(i);

            // 检查是否需要跳过嵌入
            Integer skipEmbedding = metadata.getInteger(MetadataKeyConstant.SKIP_EMBEDDING);
            knowledgeSegment.setSkipEmbedding(Optional.ofNullable(skipEmbedding).orElse(0));
            knowledgeSegment.setStatus(SegmentStatus.STORED);
            knowledgeSegments.add(knowledgeSegment);
        }
        // 5. 批量保存片段
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean saveResult = knowledgeSegmentService.saveBatch(knowledgeSegments);
        Assert.isTrue(saveResult, "保存知识片段失败");
        log.info("保存知识片段耗时：{}", stopwatch.elapsed().toMillis());

        int segmentCount = knowledgeSegments.size();

        // 6. 更新文档状态为 CHUNKED
        document.setStatus(DocumentStatus.CHUNKED);
        boolean updateResult = knowledgeDocumentService.updateById(document);
        Assert.isTrue(updateResult, "更新文档状态失败");

        // 发送文档已切分事件
        publishChunkedEvent(document, segmentCount);
        return segmentCount;
    }

    /**
     * 发送文档切分完成事件
     */
    public void publishChunkedEvent(KnowledgeDocumentEntity document, int segmentCount) {
        log.info("发送文档 CHUNKED 事件，docId: {}, segmentCount: {}", document.getDocId(), segmentCount);
        DocumentChunkedEvent event = new DocumentChunkedEvent(this, document.getDocId(), segmentCount);
        eventPublisher.publishEvent(event);
    }

    @Override
    public boolean embedAndStore(KnowledgeDocumentEntity document) {
        if (document == null) {
            return false;
        }
        if (document.getStatus() == DocumentStatus.VECTOR_STORED) {
            return true;
        }
        if (document.getStatus() != DocumentStatus.CHUNKED) {
            return false;
        }

        // 分页扫描全部document_id=docId 且 status=STORED的文档片段
        LambdaQueryWrapper<KnowledgeSegmentEntity> queryWrapper = Wrappers.<KnowledgeSegmentEntity>lambdaQuery()
                .eq(KnowledgeSegmentEntity::getDocumentId, document.getDocId())
                .eq(KnowledgeSegmentEntity::getStatus, SegmentStatus.STORED)
                .eq(KnowledgeSegmentEntity::getSkipEmbedding, 0);

        Page<KnowledgeSegmentEntity> page = knowledgeSegmentService.page(new Page<>(1, 100), queryWrapper);
        while (page.getCurrent() == 1 || page.hasNext()) {
            List<KnowledgeSegmentEntity> textSegmentsToEmbed = page.getRecords();
            // 过滤 embeddingId为空的
            textSegmentsToEmbed = textSegmentsToEmbed.stream()
                    .filter(segment -> !StringUtils.hasText(segment.getEmbeddingId()))
                    .toList();
            List<TextSegment> textSegments = textSegmentsToEmbed.stream()
                    .map(segment -> TextSegment.from(segment.getText(), Metadata.from(segment.getMetadataMap())))
                    .toList();
            // 获取嵌入向量
            Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll(textSegments);
            // 存储嵌入向量
            List<String> embeddingIds = embeddingStore.addAll(embeddingResponse.content(), textSegments);

            // todo 事务处理
            for (int i = 0; i < textSegmentsToEmbed.size(); i++) {
                String embeddingId = embeddingIds.get(i);
                KnowledgeSegmentEntity knowledgeSegment = textSegmentsToEmbed.get(i);
                knowledgeSegment.setEmbeddingId(embeddingId);
                knowledgeSegment.setStatus(SegmentStatus.VECTOR_STORED);
            }
            knowledgeSegmentService.updateBatchById(textSegmentsToEmbed);

            page = knowledgeSegmentService.page(new Page<>(page.getCurrent() + 1, 100), queryWrapper);
        }

        // double check
        long segmentCount = knowledgeSegmentService.count(queryWrapper);
        if (segmentCount == 0) {
            // 更新文档状态
            document.setStatus(DocumentStatus.VECTOR_STORED);
            return knowledgeDocumentService.updateById(document);
        }
        log.warn("向量存储失败, 存在部分分段没有存储成功，未成功的数量：{}", segmentCount);
        return false;
    }

    /**
     * 从MinIO URL中提取对象名称
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // URL格式: http://endpoint/bucketName/objectName
        int lastSlashIndex = url.lastIndexOf(bucketName) + bucketName.length();
        if (lastSlashIndex == -1 || lastSlashIndex == url.length() - 1) {
            return null;
        }
        return url.substring(lastSlashIndex + 1);
    }
}
