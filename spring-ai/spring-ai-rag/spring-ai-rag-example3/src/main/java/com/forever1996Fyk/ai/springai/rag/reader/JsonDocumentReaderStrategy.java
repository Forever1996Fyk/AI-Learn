package com.forever1996Fyk.ai.springai.rag.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/25 22:11
 **/
@Component
public class JsonDocumentReaderStrategy implements DocumentReaderStrategy {
    @Override
    public boolean support(File file) {
        String filename = file.getName().toLowerCase();
        return filename.endsWith(".json");
    }

    @Override
    public List<Document> read(File file) throws IOException {
        FileSystemResource resource = new FileSystemResource(file);
        // 假设目标提取json的两个字段description和content
        DocumentReader jsonReader = new JsonReader(resource, "description", "content");
        return jsonReader.read();
    }
}
