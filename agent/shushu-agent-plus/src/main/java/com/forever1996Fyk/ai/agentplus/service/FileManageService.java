package com.forever1996Fyk.ai.agentplus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.forever1996Fyk.ai.agentplus.domain.entity.AgentxFile;
import com.forever1996Fyk.ai.agentplus.mapper.AgentxFileMapper;
import com.forever1996Fyk.ai.agentplus.splitter.OverlapParagraphTextSplitter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/21 15:25
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class FileManageService {
    private static final Set<String> TEXT_EXTS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "csv", "json", "html", "htm", "xml",
            "rtf", "odt", "ods", "odp", "eml", "msg", "log", "yml", "yaml", "java", "py"
    );
    private static final Set<String> IMAGE_EXTS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    private final DataSource dataSource;
    private final MinioService minioService;
    private final EmbeddingService embeddingService;
    private final FileParserService fileParserService;

    private final AgentxFileMapper agentxFileMapper;

    @Value("${file.large-file-threshold:5000}")
    private int largeFileThreshold;

    @PostConstruct
    void ensureTableExists() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS agentx_file (
                    id              BIGINT       NOT NULL,
                    file_id         VARCHAR(64)  NOT NULL,
                    file_name       VARCHAR(255) NOT NULL,
                    file_type       VARCHAR(32),
                    file_size       BIGINT,
                    minio_path      VARCHAR(512),
                    extracted_text  MEDIUMTEXT,
                    status          VARCHAR(16)  NOT NULL,
                    embed           TINYINT      NOT NULL DEFAULT 0,
                    conversation_id VARCHAR(64),
                    session_id      BIGINT,
                    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_file_id (file_id)
                ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """);
        // 旧表升级：补加 session_id 列 + 索引（幂等：列已存在则跳过）
        tryExecute(jdbc, "ALTER TABLE agentx_file ADD COLUMN session_id BIGINT DEFAULT NULL");
        tryExecute(jdbc, "CREATE INDEX idx_agentx_file_session ON agentx_file (session_id)");
        // conversation_id 索引：stream 启动时按会话反查文件用
        tryExecute(jdbc, "CREATE INDEX idx_agentx_file_conv ON agentx_file (conversation_id)");
        log.info("[shushu-agent-plus] 表 agentx_file 已就绪");
    }

    /**
     * 静默执行 DDL（建索引 / 补列），失败不阻断启动（旧表已存在列/索引时正常跳过）。
     */
    private void tryExecute(JdbcTemplate jdbc, String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception e) {
            log.debug("[shushu-agent-plus] DDL 跳过（可能已存在）: {} | err={}", sql, e.getMessage());
        }
    }

    /**
     * 把一组文件关联到会话（按 conversation_id）。
     * 由 ShushuAgent 在 streamForResult / resumeStream 启动时调用：
     * 此时 conversationId 已经确定（前端生成），立即写入让后续轮次能按 conversation_id 反查。
     * session_id 仍然在 Complete 事件后补写（用于历史回放按轮次取附件）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void linkToConversation(List<String> fileIds, String conversationId) {
        if (fileIds == null || fileIds.isEmpty() || StringUtils.isBlank(conversationId)) {
            return;
        }
        LambdaUpdateWrapper<AgentxFile> wrapper = new LambdaUpdateWrapper<AgentxFile>()
                .in(AgentxFile::getFileId, fileIds)
                .set(AgentxFile::getConversationId, conversationId);
        int updated = agentxFileMapper.update(null, wrapper);
        log.info("[shushu-agent-plus] 已 link 文件到会话: convId={}, files={}, updated={}",
                conversationId, fileIds.size(), updated);
    }

    /**
     * 把一组文件关联到某次 agentx_session 轮次 + 所属会话。
     * 由 ShushuAgent 在 stream Complete 事件中调用（Complete 携带 sessionId + conversationId）。
     */
    public void linkToSession(List<String> fileIds, Long sessionId, String conversationId) {
        if (fileIds == null || fileIds.isEmpty() || sessionId == null) {
            return;
        }
        LambdaUpdateWrapper<AgentxFile> wrapper = new LambdaUpdateWrapper<AgentxFile>()
                .in(AgentxFile::getFileId, fileIds)
                .set(AgentxFile::getSessionId, sessionId);
        if (conversationId != null && !conversationId.isBlank()) {
            wrapper.set(AgentxFile::getConversationId, conversationId);
        }
        int updated = agentxFileMapper.update(null, wrapper);
        log.info("[shushu-agent-plus] 已 link 文件到 session: sessionId={}, convId={}, files={}, updated={}",
                sessionId, conversationId, fileIds.size(), updated);
    }

    /**
     * 按会话 id 查询所有已成功处理的文件（按创建时间升序）。
     * ShushuAgent.buildInstructions 用它拼 system prompt 的"当前会话文件"段。
     */
    public List<AgentxFile> listByConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        return agentxFileMapper.selectList(
                new LambdaQueryWrapper<AgentxFile>()
                        .eq(AgentxFile::getConversationId, conversationId)
                        .eq(AgentxFile::getStatus, "SUCCESS")
                        .orderByAsc(AgentxFile::getCreatedAt));
    }


    @Transactional(rollbackFor = Exception.class)
    public AgentxFile uploadFile(MultipartFile file) {
        String fileId = UUID.randomUUID().toString();
        String fileType = extractExt(file.getOriginalFilename());
        long fileSize = file.getSize();

        log.info("[shushu-agent-plus] 开始上传: fileId={}, name={}, type={}, size={}",
                fileId, file.getOriginalFilename(), fileType, fileSize);

        AgentxFile entity = new AgentxFile();
        entity.setFileId(fileId);
        entity.setFileName(file.getOriginalFilename());
        entity.setFileType(fileType);
        entity.setFileSize(fileSize);
        entity.setStatus("PROCESSING");
        entity.setEmbed(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        agentxFileMapper.insert(entity);

        try {
            // 上传 MinIO
            String objectName = generateObjectName(fileId, fileType);
            String minioPath = minioService.uploadFile(file, objectName);
            entity.setMinioPath(minioPath);

            // 按类型解析
            if (isTextFile(fileType)) {
                handleTextFile(file, entity);
            } else if (isImageFile(fileType)) {
                // 图片不在上传时识别（避免拖慢上传 + 烧模型成本），留给 analyzeFile 按需调多模态
                log.info("[shushu-agent-plus] 图片文件仅落 MinIO + DB，识别延迟到 analyzeFile: fileId={}", fileId);
            } else {
                log.info("[shushu-agent-plus] 不支持的解析类型: {}，仅存储 MinIO 元数据", fileType);
            }

            entity.setStatus("SUCCESS");
            entity.setUpdatedAt(LocalDateTime.now());
            agentxFileMapper.updateById(entity);
            log.info("[shushu-agent-plus] 上传成功: fileId={}, embed={}", fileId, entity.getEmbed());
            return entity;

        } catch (Exception e) {
            log.error("[shushu-agent-plus] 上传失败: fileId={}", fileId, e);
            entity.setStatus("FAILED");
            entity.setUpdatedAt(LocalDateTime.now());
            agentxFileMapper.updateById(entity);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    public AgentxFile getFileInfo(String fileId) {
        AgentxFile entity = findByFileId(fileId);
        if (entity == null) {
            throw new IllegalArgumentException("文件不存在: " + fileId);
        }
        return entity;
    }


    /**
     * 从 MinIO 下载文件原始字节（analyzeFile 调多模态识别时用）。
     */
    public byte[] downloadFileBytes(String fileId) {
        return downloadFileBytes(getFileInfo(fileId));
    }

    /**
     * 从 MinIO 下载文件原始字节（analyzeFile 调多模态识别时用）。
     */
    public byte[] downloadFileBytes(AgentxFile file) {
        if (StringUtils.isBlank(file.getMinioPath())) {
            throw new IllegalStateException("文件未上传到对象存储: " + file.getFileId());
        }
        String objectName = extractObjectName(file.getMinioPath());
        try (InputStream is = minioService.downloadFile(objectName)){
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("从 MinIO 下载文件失败: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileId) {
        AgentxFile entity = findByFileId(fileId);
        if (entity == null) {
            throw new IllegalArgumentException("文件不存在: " + fileId);
        }
        try {
            if (StringUtils.isNotBlank(entity.getMinioPath())) {
                String objectName = extractObjectName(entity.getMinioPath());
                minioService.deleteFile(objectName);
            }
        } catch (Exception e) {
            log.warn("[dodo-agentx] MinIO 删除失败（继续删 DB）: fileId={}, err={}", fileId, e.getMessage());
        }
        agentxFileMapper.delete(new QueryWrapper<AgentxFile>().eq("file_id", fileId));
        log.info("[dodo-agentx] 文件已删除: fileId={}", fileId);
    }

    /**
     * 把图片识别后的描述写回 extracted_text（懒缓存）。
     * 下次 analyzeFile 同一图片直接走 cache，不再调多模态。
     */
    public void saveExtractedText(String fileId, String text) {
        AgentxFile entity = findByFileId(fileId);
        if (entity == null) {
            return;
        }
        entity.setExtractedText(text);
        entity.setUpdatedAt(LocalDateTime.now());
        agentxFileMapper.updateById(entity);
    }

    private AgentxFile findByFileId(String fileId) {
        return agentxFileMapper.selectOne(
                new QueryWrapper<AgentxFile>().eq("file_id", fileId));
    }


    private void handleTextFile(MultipartFile file, AgentxFile entity) {
        var parseResult = fileParserService.parse(file);
        String fullText = parseResult.fullText();
        String truncated = parseResult.truncatedText();

        entity.setExtractedText(truncated);

        if (isLargeFile(fullText) && embeddingService.isAvailable()) {
            try {
                List<Document> chunks = new OverlapParagraphTextSplitter(500, 50)
                        .apply(List.of(new Document(fullText)));
                embeddingService.embedAndStore(chunks, entity.getFileId());
                entity.setEmbed(1);
                log.info("[shushu-agent-plus] 大文件已向量化: fileId={}, chunks={}", entity.getFileId(), chunks.size());
            } catch (Exception e) {
                log.warn("[shushu-agent-plus] 向量化失败，回退到直接加载: fileId={}, err={}", entity.getFileId(), e.getMessage());
                // embed 保持 0，不阻断上传
            }
        } else if (isLargeFile(fullText)) {
            log.info("[shushu-agent-plus] 大文件但 PgVector 不可用，仅存截断文本: fileId={}", entity.getFileId());
        }
    }

    private boolean isLargeFile(String text) {
        return text != null && text.length() >= largeFileThreshold;
    }

    private static String extractExt(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String generateObjectName(String fileId, String fileType) {
        return "file-" + fileId.replace("-", "") + "." + fileType;
    }

    private static String extractObjectName(String fullPath) {
        if (fullPath == null || !fullPath.contains("/")) {
            return fullPath;
        }
        return fullPath.substring(fullPath.lastIndexOf('/') + 1);
    }

    public boolean isTextFile(String ext) {
        return ext != null && TEXT_EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public boolean isImageFile(String ext) {
        return ext != null && IMAGE_EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }
}
