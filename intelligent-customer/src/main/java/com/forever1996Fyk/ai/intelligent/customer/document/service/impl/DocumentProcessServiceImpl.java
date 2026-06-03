package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentUploadParam;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import com.forever1996Fyk.ai.intelligent.customer.document.factory.FileProcessServiceFactory;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileStorageService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import com.forever1996Fyk.ai.intelligent.customer.document.util.FileTypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;

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
    private FileProcessServiceFactory fileProcessServiceFactory;

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
}
