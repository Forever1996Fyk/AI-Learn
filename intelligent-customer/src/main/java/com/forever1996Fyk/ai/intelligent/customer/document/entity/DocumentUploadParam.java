package com.forever1996Fyk.ai.intelligent.customer.document.entity;

import org.springframework.web.multipart.MultipartFile;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 11:02
 **/
public record DocumentUploadParam(MultipartFile file, String uploadUser, String title, String accessibleBy,
                                  String description, String knowledgeBaseType, String tableName) {
}
