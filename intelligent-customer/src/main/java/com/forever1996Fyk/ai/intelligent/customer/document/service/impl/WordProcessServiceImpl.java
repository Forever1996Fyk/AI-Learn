package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.FileType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/5 21:16
 **/
@Slf4j
@Component
public class WordProcessServiceImpl extends MinerUFileProcessBaseServiceImpl{
    @Override
    public boolean supports(FileType fileType, KnowledgeBaseType baseType) {
        return fileType == FileType.DOC;
    }
}
