package com.forever1996Fyk.ai.agentplus.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * @program: AI-Learn
 * @description:
 * 文件解析服务（基于 Apache Tika）。
 * 统一入口，自动适配 PDF / Office 全家桶 / HTML / Email / 纯文本 等几十种格式。
 * 替代 dodo-agent 中的 PDFBox + POI 手写 switch-case。
 * @author: YuKai Fan
 * @create: 2026/9/21 15:26
 **/
@Slf4j
@Service
public class FileParserService {
    @Value("${file.text-max-length:20000}")
    private int maxTextLength;

    private final Tika tika = new Tika();

    /**
     * 解析文件，返回全量文本 + 截断版（避免大文件塞爆 DB / LLM 上下文）。
     * 全量文本供向量化使用，截断文本供直接加载展示。
     */
    public ParseResult parse(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();
        log.info("[dodo-agentx] Tika 开始解析: {} ({} bytes)", fileName, fileSize);

        try (InputStream is = file.getInputStream()) {
            String fullText = tika.parseToString(is);
            if (fullText == null) {
                fullText = "";
            }
            fullText = fullText.trim();

            String truncated = truncate(fullText);
            log.info("[dodo-agentx] Tika 解析完成: {} | 全量长度={} | 截断长度={}",
                    fileName, fullText.length(), truncated.length());
            return new ParseResult(fullText, truncated);
        } catch (Exception e) {
            log.error("[dodo-agentx] Tika 解析失败: {}", fileName, e);
            throw new RuntimeException("文件解析失败: " + e.getMessage(), e);
        }
    }

    private String truncate(String text) {
        if (text.length() <= maxTextLength) {
            return text;
        }
        log.warn("[dodo-agentx] 文本超过 {} 字符，截断存储", maxTextLength);
        return text.substring(0, maxTextLength) + "\n\n... (内容已截断，文件过长；细节可通过 RAG 检索)";
    }

    /**
     * 解析结果
     * @param fullText    全量文本，用于向量化
     * @param truncatedText 截断文本，用于直接加载展示
     */
    public record ParseResult(
            String fullText,
            String truncatedText) {
    }
}
