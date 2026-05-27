package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.transformer.splitter.RecursiveCharacterTextSplitter;
import com.forever1996Fyk.ai.springai.rag.embedding.EmbeddingService;
import com.forever1996Fyk.ai.springai.rag.reader.factory.DocumentReaderStrategyFactory;
import com.forever1996Fyk.ai.springai.rag.service.DocumentCleaner;
import com.forever1996Fyk.ai.springai.rag.splitter.OverlapParagraphTextSplitter;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/26 22:36
 **/
@RestController
@RequestMapping("/rag/embedding")
public class RagEmbeddingController {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private DocumentReaderStrategyFactory documentReaderStrategyFactory;

    @GetMapping("/test")
    public String test() {
        float[] vector = embeddingModel.embed("test");
        System.out.println("向量长度：" + vector.length);
        for (float v : vector) {
            System.out.println("向量：" + v);
        }
        return "success";
    }

    @RequestMapping("embed")
    public String embed(String filePath) {
        List<Document> documents;
        try {
            documents = documentReaderStrategyFactory.read(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 数据清洗，避免存储无用符号
        List<Document> cleanDocuments = DocumentCleaner.cleanDocuments(documents);
        List<Document> allChunkedDocuments = cleanDocuments.stream()
                .flatMap(document -> {
                    // 这里使用递归分词
                    OverlapParagraphTextSplitter splitter = new OverlapParagraphTextSplitter(100, 5);
                    return splitter.split(document).stream();
                })
                .collect(Collectors.toList());
        embeddingService.embedAndStore(allChunkedDocuments);
        return "success";
    }
}
