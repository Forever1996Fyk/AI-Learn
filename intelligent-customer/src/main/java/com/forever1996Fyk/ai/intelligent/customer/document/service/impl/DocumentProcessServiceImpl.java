package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentSplitParam;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentUploadParam;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.FileType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.SegmentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.event.DocumentChunkedEvent;
import com.forever1996Fyk.ai.intelligent.customer.document.factory.FileProcessServiceFactory;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentVersionEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeSegmentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileStorageService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentVersionService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.document.util.FileTypeUtils;
import com.forever1996Fyk.ai.intelligent.customer.document.util.VersionUtil;
import com.forever1996Fyk.ai.intelligent.customer.infra.lock.DistributeLock;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.spltter.DocumentSplitterFactory;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.spltter.ExcelSplitter;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
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
    private KnowledgeDocumentVersionService knowledgeDocumentVersionService;

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;
    @Autowired
    private ElasticsearchEmbeddingStore embeddingStore;


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 上传文件
     * 这里需要考虑单个用户并发上传文件，所以需要加锁，key: 表示当前上传的用户，这样一个用户同一时间只能上传一次
     *
     * @param documentUploadParam DocumentUploadParam
     * @return KnowledgeDocumentEntity
     */
    @Override
    @DistributeLock(scene = "document-upload", keyExpression = "#documentUploadParam.uploadUser", waitTime = 0)
    public KnowledgeDocumentEntity upload(DocumentUploadParam documentUploadParam) throws IOException {
        // 计算文件内容hash，用于去重
        String contentHash = calculateContentHash(documentUploadParam.file());

        // 检查是否已存在相同内容的版本（跨文档跨版本去重）
        if (knowledgeDocumentVersionService.existsByContentHash(contentHash)) {
            throw new IllegalArgumentException("文档内容已存在，请勿重复上传");
        }
        // 创建文档记录
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity().create(documentUploadParam);
        boolean result = knowledgeDocumentService.save(document);
        Assert.isTrue(result, "文件上传失败");

        log.info("start to upload");
        String fileName = documentUploadParam.file().getOriginalFilename();
        String fileUrl;
        try {
            // minio 上传
            fileUrl = fileStorageService.uploadFile(documentUploadParam.file(), fileName);
        } catch (Exception e) {
            knowledgeDocumentService.removeDocumentWithSegments(document.getDocId());
            log.info("文件上传失败，文档已删除");
            return null;
        }

        // 创建初始版本记录
        KnowledgeDocumentVersionEntity versionRecord = createVersionRecord(
                document.getDocId(), documentUploadParam.version(), fileUrl, null,
                documentUploadParam.uploadUser(), contentHash, DocumentStatus.UPLOADED, null);
        document.setCurrentVersionId(versionRecord.getVersionId());

        // 处理文档（转换/存储），获取转换后的文档URL
        String convertedDocUrl = processFile(fileName, documentUploadParam.file(), document, fileUrl);

        // 更新版本记录的转换后URL
        versionRecord = knowledgeDocumentVersionService.getById(versionRecord.getVersionId());
        versionRecord.setConvertedDocUrl(convertedDocUrl);
        result = knowledgeDocumentVersionService.updateById(versionRecord);
        Assert.isTrue(result, "版本记录更新失败");

        KnowledgeDocumentEntity documentInDb = knowledgeDocumentService.getById(document.getDocId());
        documentInDb.setCurrentVersionId(versionRecord.getVersionId());
        result = knowledgeDocumentService.updateById(documentInDb);
        Assert.isTrue(result, "文档当前版本更新失败");
        return document;
    }

    /**
     * 处理文档（转换/存储）
     */
    private String processFile(String fileName, MultipartFile file, KnowledgeDocumentEntity document, String fileUrl) throws IOException {
        String convertedDocUrl;
        FileProcessService fileProcessService = fileProcessServiceFactory.get(FileTypeUtils.getFileType(fileName, file), document.getKnowledgeBaseType());
        if (fileProcessService != null) {
            convertedDocUrl = fileProcessService.processDocument(document, file.getInputStream());
        } else {
            DocumentStatus targetStatus = document.getKnowledgeBaseType() == KnowledgeBaseType.DOCUMENT_SEARCH
                    ? DocumentStatus.CONVERTED : DocumentStatus.STORED;
            knowledgeDocumentService.advanceDocumentAndVersionStatus(document.getDocId(), document.getCurrentVersionId(), targetStatus);
            document.setStatus(targetStatus);
            convertedDocUrl = fileUrl;
        }
        return convertedDocUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributeLock(scene = "document-split", keyExpression = "#document.docId", waitTime = 0)
    public int split(KnowledgeDocumentEntity document, DocumentSplitParam documentSplitParam) {
        // 1. 查询文档
        Assert.notNull(document, "文档不存在");

        // 从版本表获取当前版本的文件URL
        KnowledgeDocumentVersionEntity versionRecord = knowledgeDocumentVersionService.getById(document.getCurrentVersionId());
        Assert.notNull(versionRecord, "文档版本不存在");
        Assert.notNull(versionRecord.getConvertedDocUrl(), "文档未转换完成");

        if (DocumentStatus.CHUNKED == versionRecord.getStatus()) {
            //返回已切分的分段数量
            long count = knowledgeSegmentService.count(
                    new QueryWrapper<KnowledgeSegmentEntity>().lambda()
                            .eq(KnowledgeSegmentEntity::getDocumentId, document.getDocId())
                            .eq(KnowledgeSegmentEntity::getDocumentVersion, document.getCurrentVersionId())
                            .eq(KnowledgeSegmentEntity::getSkipEmbedding, 0)
            );
            return Math.toIntExact(count);
        }

        if (versionRecord.getStatus() != DocumentStatus.CONVERTED) {
            throw new RuntimeException("文档状态不是CONVERTED，无法切分");
        }

        // 2.从 MinIO下载文件内容
        String convertedDocUrl = versionRecord.getConvertedDocUrl();
        String objectName = extractObjectNameFromUrl(convertedDocUrl);
        Assert.notNull(objectName, "无法解析文档 URL");

        List<KnowledgeSegmentEntity> knowledgeSegments = Lists.newArrayList();
        List<TextSegment> segments;
        try (InputStream inputStream = fileStorageService.downloadFile(objectName)) {
            //EXCEL单独处理，因为他不是Document类型
            if (FileType.EXCEL == FileTypeUtils.getFileType(versionRecord.getConvertedDocUrl())
                    || FileType.CSV == FileTypeUtils.getFileType(versionRecord.getConvertedDocUrl())
            ) {
                ExcelSplitter excelSplitter = new ExcelSplitter(documentSplitParam.chunkSize(), false);
                segments = excelSplitter.split(inputStream.readAllBytes());
            } else {
                DocumentSplitter splitter = DocumentSplitterFactory.getInstance(documentSplitParam);
                Document doc = Document.from(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                segments = splitter.split(doc);
            }
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
            knowledgeSegment.setMetadata(enrichMetadata(document, versionRecord, metadata));
            knowledgeSegment.setDocumentId(document.getDocId());
            knowledgeSegment.setDocumentVersion(document.getCurrentVersionId());
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

        // 6. 更新文档状态为 CHUNKED，并保存分段参数
        boolean advanceResult = knowledgeDocumentService.advanceDocumentAndVersionStatus(document.getDocId(), document.getCurrentVersionId(), DocumentStatus.CHUNKED);
        Assert.isTrue(advanceResult, "更新文档版本状态失败");

        // 发送文档已切分事件
        publishChunkedEvent(document, segmentCount);
        return segmentCount;
    }

    /**
     * 发送文档切分完成事件
     */
    public void publishChunkedEvent(KnowledgeDocumentEntity document, int segmentCount) {
        log.info("发送文档 CHUNKED 事件，docId: {}, segmentCount: {}", document.getDocId(), segmentCount);
        DocumentChunkedEvent event = new DocumentChunkedEvent(this, document.getDocId(), document.getCurrentVersionId(), segmentCount);
        eventPublisher.publishEvent(event);
    }

    @Override
    @DistributeLock(scene = "document-split", keyExpression = "#document.docId", waitTime = 0)
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

    @Override
    @DistributeLock(scene = "document-embed", keyExpression = "#documentVersion.versionId", waitTime = 0)
    public boolean embedAndStore(KnowledgeDocumentVersionEntity documentVersion) {
        if (documentVersion == null) {
            return false;
        }
        if (documentVersion.getStatus() == DocumentStatus.VECTOR_STORED) {
            log.info("文档版本状态已为VECTOR_STORED，无需重复向量化: {}", documentVersion.getVersionId());
            return true;
        }
        if (documentVersion.getStatus() != DocumentStatus.CHUNKED) {
            log.warn("文档版本状态不是CHUNKED，无法完成向量化: {}", documentVersion.getStatus());
            return false;
        }

        knowledgeDocumentService.activateVersion(documentVersion.getVersionId());

        //double check
        long segmentCount = knowledgeSegmentService.count(
                new QueryWrapper<KnowledgeSegmentEntity>().lambda()
                        .eq(KnowledgeSegmentEntity::getDocumentId, documentVersion.getDocId())
                        .eq(KnowledgeSegmentEntity::getDocumentVersion, documentVersion.getVersionId())
                        .eq(KnowledgeSegmentEntity::getStatus, SegmentStatus.STORED)
                        .eq(KnowledgeSegmentEntity::getSkipEmbedding, 0)
        );

        if (segmentCount == 0) {
            // 针对非当前版本的文档，取消激活
            List<KnowledgeDocumentVersionEntity> documentVersions = knowledgeDocumentVersionService.list(
                    new QueryWrapper<KnowledgeDocumentVersionEntity>().lambda()
                            .eq(KnowledgeDocumentVersionEntity::getDocId, documentVersion.getDocId())
                            .eq(KnowledgeDocumentVersionEntity::getStatus, DocumentStatus.VECTOR_STORED)
                            .ne(KnowledgeDocumentVersionEntity::getVersionId, documentVersion.getVersionId())
            );

            documentVersions.forEach(version -> knowledgeDocumentService.deactivateVersion(version.getVersionId()));
            return true;
        }

        log.warn("向量存储失败，存在部分分段没有存储成功，未成功的数量： " + segmentCount);
        return false;
    }

    @Override
    @DistributeLock(scene = "document-upload", keyExpression = "#docId", waitTime = 0)
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentEntity switchVersion(Long docId, Long versionId) {
        // 查询文档
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(docId);
        Assert.notNull(document, "文档不存在");

        // 查询目标版本
        KnowledgeDocumentVersionEntity versionRecord = knowledgeDocumentVersionService.getById(versionId);
        Assert.notNull(versionRecord, "版本不存在");
        Assert.isTrue(versionRecord.getDocId().equals(docId), "版本不属于该文档");

        // 如果已经是当前版本，无需切换
        if (versionId.equals(document.getCurrentVersionId())) {
            return document;
        }

        log.info("切换文档 {} 的版本：从 versionId={} 切换到 versionId={}", docId, document.getCurrentVersionId(), versionId);

        // DATA_QUERY 类型文档无分段/向量，直接切换当前版本即可
        if (document.getKnowledgeBaseType() == KnowledgeBaseType.DATA_QUERY) {
            // DATA_QUERY 类型不支持回退到旧版本，只能保持为最新版本
            String latestVersion = knowledgeDocumentVersionService.getLatestVersion(docId);
            if (latestVersion != null && VersionUtil.compareVersions(versionRecord.getVersion(), latestVersion) < 0) {
                throw new IllegalArgumentException("DATA_QUERY 类型文档不支持切换到旧版本");
            }
            document.setCurrentVersionId(versionId);
            boolean docUpdateResult = knowledgeDocumentService.updateById(document);
            Assert.isTrue(docUpdateResult, "更新文档版本失败");
            return document;
        }

        // 更新原版本文档片段状态为 STORED
        LambdaUpdateWrapper<KnowledgeSegmentEntity> updateWrapper = Wrappers.<KnowledgeSegmentEntity>lambdaUpdate()
                .set(KnowledgeSegmentEntity::getStatus, SegmentStatus.STORED)
                .eq(KnowledgeSegmentEntity::getDocumentId, document.getDocId())
                .eq(KnowledgeSegmentEntity::getDocumentVersion, document.getCurrentVersionId());
        boolean segAffected = knowledgeSegmentService.update(null, updateWrapper);
        log.info("切换版本：旧版本分段状态降级完成, affected={}", segAffected);

        boolean embedResult = embedAndStore(versionRecord);
        Assert.isTrue(embedResult, "更新文档片段状态失败");

        // 更新文档版本
        document.setCurrentVersionId(versionId);
        boolean docUpdateResult = knowledgeDocumentService.updateById(document);
        Assert.isTrue(docUpdateResult, "更新文档版本失败");

        return document;
    }

    @Override
    @DistributeLock(scene = "document-upload", keyExpression = "#uploadUser", waitTime = 0)
    public KnowledgeDocumentEntity uploadNewVersion(Long docId, String version, MultipartFile file, String uploadUser, String changelog) throws IOException {
        // 查询文档
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(docId);
        Assert.notNull(document, "文档不存在");

        // 校验版本号必须大于已有最大版本号
        String latestVersion = knowledgeDocumentVersionService.getLatestVersion(docId);
        if (latestVersion != null && VersionUtil.compareVersions(version, latestVersion) <= 0) {
            throw new IllegalArgumentException("版本号 " + version + " 不大于现有最新版本号 " + latestVersion + "，请使用更大的版本号");
        }
        // 计算文件内容hash，用于去重
        String contentHash = calculateContentHash(file);
        // 检查是否已存在相同内容的版本（跨文档跨版本去重）
        if (knowledgeDocumentVersionService.existsByContentHash(contentHash)) {
            throw new IllegalArgumentException("文档内容已存在，请勿重复上传");
        }

        KnowledgeDocumentVersionEntity versionRecord = null;
        log.info("start to upload version {} for doc {} ....", version, docId);

        // 1. 上传新版本文件到MinIO（不清理旧版本数据，保证处理期间旧版本仍可查询）
        String fileName = file.getOriginalFilename();
        String fileUrl = null;
        try {
            fileUrl = fileStorageService.uploadFile(file, fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 2. 先创建新版本记录，使 processDocument 内部可以推进版本状态
        versionRecord = createVersionRecord(
                document.getDocId(), version, fileUrl, null,
                uploadUser, contentHash, DocumentStatus.UPLOADED, changelog);

        // 这一步先不更新数据库，只是为了让后续的操作能从document中取出version，避免npm和流程走不下去
        // document的更新会在最后执行，确保前置流程都完成后实现版本的切换。
        document.setCurrentVersionId(versionRecord.getVersionId());
        // 3. 处理文档（转换/存储），获取转换后的文档URL
        String convertedDocUrl = processFile(fileName, file, document, fileUrl);

        // 4. 更新版本记录的转换后URL
        versionRecord = knowledgeDocumentVersionService.getById(versionRecord.getVersionId());
        versionRecord.setConvertedDocUrl(convertedDocUrl);
        boolean result = knowledgeDocumentVersionService.updateById(versionRecord);
        Assert.isTrue(result, "版本记录更新失败");

        result = knowledgeDocumentService.updateById(document);
        Assert.isTrue(result, "文档当前版本更新失败");

        log.info("文档 {} 新版本 {} 上传完成，旧版本数据保留中，待新版本向量化完成后自动清理", docId, version);
        return document;
    }

    /**
     * 计算文件内容的SHA-256哈希值
     *
     * @param file 上传的文件
     * @return SHA-256哈希的十六进制字符串
     */
    private String calculateContentHash(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256算法不可用", e);
        }
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

    /**
     * 创建版本记录
     *
     * @param docId           文档ID
     * @param version         版本号（语义化版本，如 "1.0.0"）
     * @param docUrl          原始文档URL（MinIO）
     * @param convertedDocUrl 转换后的文档URL
     * @param uploadUser      上传用户
     * @param contentHash     内容哈希
     * @param status          文档状态
     * @param changelog       变更说明
     * @return 保存后的版本记录
     */
    private KnowledgeDocumentVersionEntity createVersionRecord(Long docId, String version, String docUrl,
                                                               String convertedDocUrl, String uploadUser,
                                                               String contentHash, DocumentStatus status, String changelog) {
        KnowledgeDocumentVersionEntity versionRecord = new KnowledgeDocumentVersionEntity();
        versionRecord.setDocId(docId);
        versionRecord.setVersion(version);
        versionRecord.setDocUrl(docUrl);
        versionRecord.setConvertedDocUrl(convertedDocUrl);
        versionRecord.setContentHash(contentHash);
        versionRecord.setStatus(status);
        versionRecord.setUploadUser(uploadUser);
        versionRecord.setChangelog(changelog);
        knowledgeDocumentVersionService.save(versionRecord);
        log.info("创建版本记录成功, docId: {}, version: {}, versionId: {}",
                docId, version, versionRecord.getVersionId());
        return versionRecord;
    }

    /**
     * 填充元数据
     *
     * @param document 文档信息
     * @param metadata 元数据
     * @return
     */
    private static String enrichMetadata(KnowledgeDocumentEntity document, KnowledgeDocumentVersionEntity versionRecord, Metadata metadata) {
        metadata.put(MetadataKeyConstant.DOC_ID, document.getDocId());
        metadata.put(MetadataKeyConstant.FILE_NAME, document.getDocTitle());
        metadata.put(MetadataKeyConstant.URL, versionRecord.getDocUrl());
        if (document.getCurrentVersionId() != null) {
            metadata.put(MetadataKeyConstant.VERSION, document.getCurrentVersionId());
        }
        Map<String, Object> metadataMap = metadata.toMap();
        metadataMap.put(MetadataKeyConstant.ACCESSIBLE_BY, document.getAccessibleBy());

        return JSON.toJSONString(metadataMap);
    }
}
