package com.forever1996Fyk.ai.springai.rag.reader.factory;

import com.forever1996Fyk.ai.springai.rag.reader.DocumentReaderStrategy;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/25 22:30
 **/
@Component
public class DocumentReaderStrategyFactory {

    private final List<DocumentReaderStrategy> strategies;

    public DocumentReaderStrategyFactory(List<DocumentReaderStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 根据文件类型选择对应的读取策略
     *
     * @param file file
     * @return List<Document>
     */
    public List<Document> read(File file) throws IOException {
        for (DocumentReaderStrategy strategy : strategies) {
            if (strategy.support(file)) {
                return strategy.read(file);
            }
        }
        throw new IllegalArgumentException("不支持的文件类型: " + file.getName());
    }
}
