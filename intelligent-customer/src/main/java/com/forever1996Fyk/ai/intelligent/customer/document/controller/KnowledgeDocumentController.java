package com.forever1996Fyk.ai.intelligent.customer.document.controller;

import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentSplitParam;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentUploadParam;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.impl.PdfFileProcessServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/2 22:42
 **/
@RestController
@RequestMapping("/api/document")
public class KnowledgeDocumentController {
    @Autowired
    private DocumentProcessService documentProcessService;
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private PdfFileProcessServiceImpl fileProcessService;
    /**
     * 文件上传接口
     *
     * @param file         上传的文件
     * @param uploadUser   上传用户
     * @param accessibleBy 可见范围（可选）
     * @return 保存后的文档记录
     */
    @PostMapping("/upload")
    public KnowledgeDocumentEntity uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadUser") String uploadUser,
            @RequestParam("title") String title,
            @RequestParam(value = "tableName", required = false) String tableName,
            @RequestParam("description") String description,
            @RequestParam("knowledgeBaseType") String knowledgeBaseType,
            @RequestParam(value = "accessibleBy", required = false) String accessibleBy,
            @RequestParam(value = "version", required = false, defaultValue = "1.0.0") String version) throws IOException {
        return documentProcessService.upload(new DocumentUploadParam(file, uploadUser, title, accessibleBy, description, knowledgeBaseType, tableName, version));
    }

    /**
     * 上传文档新版本
     *
     * @param file        新版本文件
     * @param docId       文档ID（knowledge_document.doc_id）
     * @param version     新版本号（语义化版本，如 "2.0.0"，必须大于现有最新版本号）
     * @param changelog   版本变更说明（可选）
     * @return 更新后的文档记录
     */
    @PostMapping("/upload-version")
    public KnowledgeDocumentEntity uploadVersion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docId") Long docId,
            @RequestParam("version") String version,
            @RequestParam(value = "changelog", required = false) String changelog) throws IOException {
//        String uploadUser = authService.getCurrentUser().getName();
        String uploadUser = "system";
        return documentProcessService.uploadNewVersion(docId, version, file, uploadUser, changelog);
    }

    /**
     * 对文档进行切分
     * 注意：此方法为手动触发切分接口，正常流程由事件驱动自动执行
     *
     * @param documentId 文档ID
     * @return 切分后的片段数量
     */
    @PostMapping("/split/{documentId}")
    public Integer splitDocument(@PathVariable Long documentId,
                                 @RequestParam("splitType") String splitType,
                                 @RequestParam("chunkSize") Integer chunkSize,
                                 @RequestParam(value = "overlap", required = false) Integer overlap,
                                 @RequestParam(value = "regex", required = false) String regex,
                                 @RequestParam(value = "titleLevel", required = false) Integer titleLevel,
                                 @RequestParam(value = "separator", required = false) String separator
    ) {
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(documentId);
        return documentProcessService.split(document, new DocumentSplitParam(splitType, chunkSize, overlap, titleLevel, separator, regex));
    }

    /**
     * 向量化并存储
     * 注意：此方法为手动触发向量化接口，正常流程由事件驱动自动执行
     *
     * @param docId 文档ID
     * @return 结果
     */
    @PostMapping("/embedding")
    public String embedding(Long docId) {
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(docId);
        return documentProcessService.embedAndStore(document) ? "success" : "failed";
    }

    /**
     * 切换文档到指定版本
     * 清理当前版本的分段和向量，恢复目标版本的文件URL和状态，状态置为 CONVERTED 等待重新切片
     *
     * @param docId     文档ID
     * @param versionId 目标版本ID
     * @return 更新后的文档记录
     */
    @PostMapping("/switch-version")
    public KnowledgeDocumentEntity switchVersion(@RequestParam("docId") Long docId,
                                           @RequestParam("versionId") Long versionId) {
        return documentProcessService.switchVersion(docId, versionId);
    }

    /**
     * 让指定版本失效：清理该版本 ES 向量，将分段状态降为 STORED，版本状态降为 CHUNKED
     *
     * @param versionId 版本ID（knowledge_document_version.version_id）
     */
    @PostMapping("/deactivate-version")
    public void deactivateVersion(@RequestParam("versionId") Long versionId) {
        knowledgeDocumentService.deactivateVersion(versionId);
    }

    /**
     * 让指定版本生效（重新向量化）：对 STORED 分段重新 embed 写入 ES，版本状态升为 VECTOR_STORED
     *
     * @param versionId 版本ID（knowledge_document_version.version_id）
     */
    @PostMapping("/activate-version")
    public void activateVersion(@RequestParam("versionId") Long versionId) {
        knowledgeDocumentService.activateVersion(versionId);
    }

    /**
     * 获取图片描述
     * 用于测试
     *
     * @param url 图片URL
     * @return 图片描述
     */
    @GetMapping("/image-desc")
    public String getImageDesc(String url) {
        return fileProcessService.generateImageDescription(url);
    }
}
