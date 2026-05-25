package com.forever1996Fyk.ai.springai.rag.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.FileSystemResource;
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
public class TextDocumentReaderStrategy implements DocumentReaderStrategy {
    @Override
    public boolean support(File file) {
        String filename = file.getName().toLowerCase();
        return filename.endsWith(".txt") || filename.endsWith(".tex") || filename.endsWith(".text");
    }

    @Override
    public List<Document> read(File file) throws IOException {
        FileSystemResource resource = new FileSystemResource(file);
        TextReader textReader = new TextReader(resource);
        return textReader.read();
    }
}
