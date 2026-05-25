package com.forever1996Fyk.ai.springai.rag.reader;

import org.springframework.ai.document.Document;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/25 22:11
 **/
public interface DocumentReaderStrategy {

    /**
     * 判断是否支持文件
     *
     * @param file File
     * @return boolean
     */
    boolean support(File file);

    /**
     * 读取文件
     *
     * @param file File
     * @return List<Document>
     * @throws IOException IOException
     */
    List<Document> read(File file) throws IOException;
}
