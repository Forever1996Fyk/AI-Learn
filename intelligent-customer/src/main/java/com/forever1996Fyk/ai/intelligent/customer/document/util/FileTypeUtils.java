package com.forever1996Fyk.ai.intelligent.customer.document.util;

import com.forever1996Fyk.ai.intelligent.customer.document.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 15:02
 **/
@Slf4j
public class FileTypeUtils {
    private static final Tika tika = new Tika();

    public static FileType getFileType(String fileName, MultipartFile file) {
        if (file == null) {
            return null;
        }

        if (isPdfFile(fileName) || isPdfContent(file)) {
            return FileType.PDF;
        }

        return null;
    }

    private static boolean isPdfFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        return fileName.toLowerCase().endsWith(".pdf");
    }

    /**
     * 通过 Apache Tika 检测文件内容类型判断是否为 PDF 文件
     */
    private static boolean isPdfContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String mimeType = tika.detect(is);
            return "application/pdf".equals(mimeType);
        } catch (IOException e) {
            log.error("文件类型检测失败: {}", e.getMessage());
            return false;
        }
    }

}
