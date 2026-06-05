package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.FileType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/5 21:17
 **/
public class MarkdownProcessServiceImpl extends MinerUFileProcessBaseServiceImpl {
    @Override
    public boolean supports(FileType fileType, KnowledgeBaseType baseType) {
        return fileType == FileType.MARKDOWN;
    }
}
