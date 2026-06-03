package com.forever1996Fyk.ai.intelligent.customer.document.factory;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.FileType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.KnowledgeBaseType;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 15:00
 **/
@Component
public class FileProcessServiceFactory {
    @Autowired
    private List<FileProcessService> fileProcessServices;

    public FileProcessService get(FileType fileType, KnowledgeBaseType knowledgeBaseType) {
        return fileProcessServices.stream()
                .filter(fileProcessService -> fileProcessService.supports(fileType, knowledgeBaseType))
                .findFirst().orElse(null);
    }
}
