package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.FileType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import org.springframework.stereotype.Service;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 14:59
 **/
@Service
public class PdfFileProcessServiceImpl extends MinerUFileProcessBaseServiceImpl {
    @Override
    public boolean supports(FileType fileType, KnowledgeBaseType baseType) {
        return fileType == FileType.PDF;
    }
}
