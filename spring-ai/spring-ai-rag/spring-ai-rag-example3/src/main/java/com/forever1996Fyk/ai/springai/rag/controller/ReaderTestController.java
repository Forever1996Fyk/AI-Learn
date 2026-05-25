package com.forever1996Fyk.ai.springai.rag.controller;

import com.forever1996Fyk.ai.springai.rag.reader.factory.DocumentReaderStrategyFactory;
import com.forever1996Fyk.ai.springai.rag.service.DocumentCleaner;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/25 22:32
 **/
@RestController
@RequestMapping("/rag/reader")
public class ReaderTestController {

    @Autowired
    private DocumentReaderStrategyFactory documentReaderStrategyFactory;

    @GetMapping("/read")
    public List<Document> read(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + filePath);
        }
        try {
            return DocumentCleaner.cleanDocuments(documentReaderStrategyFactory.read(file));
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }
}
