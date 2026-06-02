package com.forever1996Fyk.ai.intelligent.customer.document.controller;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileStorageService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private KnowledgeDocumentService knowledgeDocumentService;
    @Autowired
    private FileStorageService fileStorageService;

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
            @RequestParam(value = "accessibleBy", required = false) String accessibleBy) throws IOException {
        try {
            String fileName = file.getOriginalFilename();
            //用minio上传
            String fileUrl = fileStorageService.uploadFile(file, fileName);

            // 构建文档记录
            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setDocTitle(fileName);
            document.setUploadUser(uploadUser);
            document.setDocUrl(fileUrl);
            document.setStatus(DocumentStatus.UPLOADED);
            //todo permission处理
            document.setAccessibleBy(accessibleBy);

            // 保存到数据库
            knowledgeDocumentService.save(document);
            return document;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
