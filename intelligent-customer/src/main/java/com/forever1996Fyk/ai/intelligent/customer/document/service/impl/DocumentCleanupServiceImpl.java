package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.document.service.DocumentCleanupService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/10 00:08
 **/
@Slf4j
@Service
public class DocumentCleanupServiceImpl implements DocumentCleanupService {
    @Autowired
    private VectorStoreService vectorStoreService;
    @Override
    public boolean cleanupOldVersionData(Long docId, Long versionId) {
        log.info("开始清理文档 {} 的旧版本数据（保留 versionId={}）", docId, versionId);

        // 清理旧版本向量
        vectorStoreService.removeByDocIdAndVersion(docId, versionId);
        log.info("清理文档 {} 旧版本向量完成", docId);
        return true;
    }
}
