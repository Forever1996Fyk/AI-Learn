package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.transformer.splitter.RecursiveCharacterTextSplitter;
import com.forever1996Fyk.ai.springai.rag.reader.factory.DocumentReaderStrategyFactory;
import com.forever1996Fyk.ai.springai.rag.service.DocumentCleaner;
import com.forever1996Fyk.ai.springai.rag.splitter.OverlapParagraphTextSplitter;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    @GetMapping("/chunk")
    public String chunk(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + filePath);
        }
        try {
            List<Document> documents = DocumentCleaner.cleanDocuments(documentReaderStrategyFactory.read(file));

            for (Document document : documents) {
                System.out.println("before chunk:" + document.getText());
                System.out.println("");
                TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
                        // 每块最多 600 tokens
                        600,
                        // 每块至少 400 字符再考虑断点
                        300,
                        // 太短的不做嵌入
                        5,
                        // 最多拆分8000块
                        8000,
                        // 保留句号、换行符
                        true
                );
                List<Document> chunkedDocuments = tokenTextSplitter.split(document);
                for (Document chunkedDocument : chunkedDocuments) {
                    System.out.println("after chunk: " + chunkedDocument.getText());
                    System.out.println("");
                }
                System.out.println("============");
            }
            return "success";
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    @GetMapping("/overlapChunk")
    public String overlapChunk(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + filePath);
        }
        try {
            List<Document> documents = DocumentCleaner.cleanDocuments(documentReaderStrategyFactory.read(file));

            for (Document document : documents) {
                System.out.println("before chunk:" + document.getText());
                System.out.println("");
                OverlapParagraphTextSplitter textSplitter = new OverlapParagraphTextSplitter(
                        // 每块最大字符数
                        100,
                        // 块之间重叠 100 字符
                        5
                );
                List<Document> chunkedDocuments = textSplitter.split(document);
                for (Document chunkedDocument : chunkedDocuments) {
                    System.out.println("after chunk: " + chunkedDocument.getText());
                    System.out.println("");
                }
                System.out.println("============");
            }
            return "success";
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    @GetMapping("/splitRecursive")
    public String splitRecursive(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + filePath);
        }
        try {
            List<Document> documents = documentReaderStrategyFactory.read(file);

            for (Document document : documents) {
                System.out.println("before chunk:" + document.getText());
                System.out.println("");
                TextSplitter textSplitter = new RecursiveCharacterTextSplitter(500, new String[]{"\n\n", "\n"});
                List<Document> chunkedDocuments = textSplitter.split(document);
                for (Document chunkedDocument : chunkedDocuments) {
                    System.out.println("after chunk: " + chunkedDocument.getText());
                    System.out.println("");
                }
                System.out.println("============");
            }
            return "success";
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

//    @GetMapping("/splitSentence")
//    public String splitSentence() {
//        DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(100, 10);
//        String[] split = sentenceSplitter.split("""
//                Harry Potter is a series of seven fantasy novels written by British author J. K. Rowling. The novels chronicle the lives of a young wizard, Harry Potter, and his friends, Ron Weasley and Hermione Granger, all of whom are students at Hogwarts School of Witchcraft and Wizardry. The main story arc concerns Harry's conflict with Lord Voldemort, a dark wizard who intends to become immortal, overthrow the wizard governing body known as the Ministry of Magic, and subjugate all wizards and Muggles (non-magical people).
//                        The series was originally published in English by Bloomsbury in the United Kingdom and Scholastic Press in the United States. A series of many genres, including fantasy, drama, coming-of-age fiction, and the British school story (which includes elements of mystery, thriller, adventure, horror, and romance), the world of Harry Potter explores numerous themes and includes many cultural meanings and references.[1] Major themes in the series include prejudice, corruption, madness, love, and death.[2]
//                """);
//        for (String s : split) {
//            System.out.println("after chunk:" + s);
//        }
//        return "success";
//    }

    public static void main(String[] args) {
        RecursiveCharacterTextSplitter textSplitter = new RecursiveCharacterTextSplitter(100);
        List<String> split = textSplitter.splitText("""
                Harry Potter is a series of seven fantasy novels written by British author J. K. Rowling. The novels chronicle the lives of a young wizard, Harry Potter, and his friends, Ron Weasley and Hermione Granger, all of whom are students at Hogwarts School of Witchcraft and Wizardry. The main story arc concerns Harry's conflict with Lord Voldemort, a dark wizard who intends to become immortal, overthrow the wizard governing body known as the Ministry of Magic, and subjugate all wizards and Muggles (non-magical people).
                        The series was originally published in English by Bloomsbury in the United Kingdom and Scholastic Press in the United States. A series of many genres, including fantasy, drama, coming-of-age fiction, and the British school story (which includes elements of mystery, thriller, adventure, horror, and romance), the world of Harry Potter explores numerous themes and includes many cultural meanings and references.[1] Major themes in the series include prejudice, corruption, madness, love, and death.[2]
                """);
        for (String s : split) {
            System.out.println("after chunk:" + s);
        }
    }
}
