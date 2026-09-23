package com.forever1996Fyk.ai.agentplus.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.forever1996Fyk.ai.agentplus.domain.dto.SkillInfo;
import com.forever1996Fyk.ai.agentplus.domain.entity.AgentxSkill;
import com.forever1996Fyk.ai.agentplus.mapper.AgentxSkillMapper;
import com.forever1996Fyk.ai.agentplus.util.FileUtils;
import com.forever1996Fyk.ai.agentplus.util.FrontmatterUtils;
import com.forever1996Fyk.ai.agentplus.util.ZipUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: AI-Learn
 * @description: Skills 管理器 — 负责上传校验、启停、删除、启用目录查询（内容存储委托给 SkillContentStore）。
 * <p>
 * deploy.mode=standalone：本地目录是权威源，手动放目录也能自动发现入库；
 * deploy.mode=cluster：DB + MinIO 是权威源，各节点从 MinIO 同步到本地。
 * 两种模式下，agent 运行时读的都是本节点 skills 目录下的内容。
 * @author: YuKai Fan
 * @create: 2026/9/21 16:53
 **/
@Slf4j
@Component
public class SkillManager {

    private static final String SKILL_FILE = "SKILL.md";

    private final Path skillsDirectory;
    private final JdbcTemplate jdbcTemplate;
    private final AgentxSkillMapper skillMapper;
    private final SkillContentStore contentStore;

    public SkillManager(@Value("${skills.directory}") String skillsDirectory,
                        DataSource dataSource,
                        AgentxSkillMapper skillMapper,
                        SkillContentStore contentStore) {
        this.skillsDirectory = Path.of(skillsDirectory);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.skillMapper = skillMapper;
        this.contentStore = contentStore;
    }

    @PostConstruct
    void init() {
        ensureTableExists();
        contentStore.sync();
    }

    /**
     * 自动建表（模仿 AgentChatMemory 模式：MySQL charset 优先，ANSI 兜底）。
     */
    private void ensureTableExists() {
        String createSql = """
                CREATE TABLE agentx_skill (
                    id           BIGINT       NOT NULL,
                    name         VARCHAR(500) NOT NULL,
                    skill_path   VARCHAR(3000) DEFAULT NULL,
                    description  VARCHAR(3000) DEFAULT '',
                    enabled      TINYINT      NOT NULL DEFAULT 1,
                    file_name    VARCHAR(255) DEFAULT NULL,
                    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id)
                )
                """;
        try {
            jdbcTemplate.execute(createSql + " DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
        } catch (Exception e) {
            try {
                jdbcTemplate.execute(createSql);
            } catch (Exception e2) {
                log.debug("[SkillManager] 建表跳过（可能已存在）: {}", e2.getMessage());
            }
        }
        try {
            jdbcTemplate.execute("CREATE UNIQUE INDEX uk_agentx_skill_name ON agentx_skill (name)");
        } catch (Exception e) {
            log.debug("[SkillManager] 唯一索引跳过（可能已存在）: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE agentx_skill ADD COLUMN skill_path VARCHAR(500) DEFAULT NULL");
            log.info("[SkillManager] 存量表已补 skill_path 列");
        } catch (Exception e) {
            log.debug("[SkillManager] skill_path 列跳过（可能已存在）: {}", e.getMessage());
        }
        log.info("[SkillManager] agentx_skill 表就绪");
    }

    /**
     * 定时同步（每 3 分钟）：localFs 实现为目录入库，minio 实现为按表从 MinIO 拉取。
     */
    @Scheduled(fixedDelay = 180000, initialDelay = 180000)
    void scheduledSync() {
        contentStore.sync();
    }


    /**
     * 返回所有 enabled=1 的 skill 目录绝对路径（供 ShushuAgent 构建 SkillsTool）。
     * 内容固定落在本节点配置的 skills 目录下，直接按名解析，与 DB 的 skill_path 无关。
     */
    public List<String> getEnabledSkillDirs() {
        List<AgentxSkill> enabled = skillMapper.selectList(
                new LambdaQueryWrapper<AgentxSkill>().eq(AgentxSkill::getEnabled, 1));
        List<String> dirs = new ArrayList<>();
        for (AgentxSkill skill : enabled) {
            String path = skillsDirectory.resolve(skill.getName()).toAbsolutePath().toString();
            if (Files.isDirectory(Path.of(path))) {
                dirs.add(path);
            } else {
                log.warn("[SkillManager] enabled skill 目录不存在: {} → {}", skill.getName(), path);
            }
        }
        return dirs;
    }

    /**
     * 列出所有 skill（含 enabled 状态 + 路径）。
     */
    public List<SkillInfo> list() {
        List<AgentxSkill> skills = skillMapper.selectList(null);
        List<SkillInfo> result = new ArrayList<>();
        for (AgentxSkill s : skills) {
            result.add(new SkillInfo(s.getName(),
                    s.getSkillPath() == null ? "" : s.getSkillPath(),
                    s.getDescription() == null ? "" : s.getDescription(),
                    s.getEnabled() != null && s.getEnabled() == 1));
        }
        return result;
    }

    /**
     * 上传 zip 压缩包，自动解压到 skills 目录。
     * 新 skill 默认启用，覆盖上传保留原 enabled 状态。
     */
    public SkillInfo uploadZip(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("仅支持 .zip 文件");
        }

        // 1. 解压到临时目录
        Files.createDirectories(skillsDirectory);
        Path tempDir = Files.createTempDirectory(skillsDirectory, "skill-upload-");
        try {
            ZipUtils.extract(file.getInputStream(), tempDir);

            // 2. 查找 SKILL.md
            Path skillMd = FileUtils.findFile(tempDir, SKILL_FILE, 3);
            if (skillMd == null) {
                throw new IllegalArgumentException("压缩包中未找到 SKILL.md 文件");
            }

            // 3. 解析 name / description
            Path skillSourceDir = skillMd.getParent();
            String content = Files.readString(skillMd);

            String name = FrontmatterUtils.extract(content, "name");
            if (StringUtils.isBlank(name)) {
                name = skillSourceDir.getFileName().toString();
            }
            name = name.trim();
            validateSkillName(name);

            String description = FrontmatterUtils.extract(content, "description");
            if (description == null) {
                description = "";
            }
            // 4. 存储内容（localFs=本地目录 / minio=MinIO+本地）
            contentStore.save(name, skillSourceDir);

            // 5. upsert DB（skill_path 存存储位置标识：本地路径 / MinIO objectKey）
            String storedPath = contentStore.storedPath(name);
            AgentxSkill existing = skillMapper.selectOne(
                    new LambdaQueryWrapper<AgentxSkill>().eq(AgentxSkill::getName, name));

            boolean enabled;
            if (existing == null) {
                AgentxSkill entity = new AgentxSkill();
                entity.setName(name);
                entity.setSkillPath(storedPath);
                entity.setDescription(description.trim());
                entity.setEnabled(1);
                entity.setFileName(originalName);
                skillMapper.insert(entity);
                enabled = true;
                log.info("[SkillManager] 新 skill 上传: {} (enabled=1)", name);
            } else {
                skillMapper.update(null, new LambdaUpdateWrapper<AgentxSkill>()
                        .eq(AgentxSkill::getName, name)
                        .set(AgentxSkill::getSkillPath, storedPath)
                        .set(AgentxSkill::getDescription, description.trim())
                        .set(AgentxSkill::getFileName, originalName));
                enabled = existing.getEnabled() != null && existing.getEnabled() == 1;
                log.info("[SkillManager] skill 覆盖上传: {} (enabled={})", name, enabled);
            }
            // 6. 通知其他节点立即同步（minio 实现广播，localFs 空操作）
            contentStore.notifyChanged();

            return new SkillInfo(name, storedPath, description.trim(), enabled);
        } finally {
            FileUtils.deleteRecursively(tempDir);
        }
    }

    /**
     * 切换 skill 启用状态。
     */
    public boolean toggleEnabled(String name, boolean enabled) {
        AgentxSkill existing = skillMapper.selectOne(
                new LambdaQueryWrapper<AgentxSkill>().eq(AgentxSkill::getName, name));
        if (existing == null) {
            throw new IllegalArgumentException("skill 不存在: " + name);
        }
        int newValue = enabled ? 1 : 0;
        if (existing.getEnabled() != null && existing.getEnabled() == newValue) {
            return enabled;
        }
        skillMapper.update(null, new LambdaUpdateWrapper<AgentxSkill>()
                .eq(AgentxSkill::getName, name)
                .set(AgentxSkill::getEnabled, newValue));
        log.info("[SkillManager] skill 切换: {} → enabled={}", name, enabled);
        return enabled;
    }

    /**
     * 删除 skill（内容 + DB 记录）。
     */
    public boolean delete(String name) throws IOException {
        validateSkillName(name);
        contentStore.delete(name);
        int deleted = skillMapper.delete(
                new LambdaQueryWrapper<AgentxSkill>().eq(AgentxSkill::getName, name));
        if (deleted > 0) {
            contentStore.notifyChanged();
        }
        log.info("[SkillManager] skill 删除: {} (db={})", name, deleted > 0);
        return deleted > 0;
    }

    /**
     * 校验 skill 名称（防路径穿越）。
     */
    private void validateSkillName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("skill 名称不能为空");
        }
        if (name.contains("..") || name.contains("/") || name.contains("\\") || name.contains(":")) {
            throw new IllegalArgumentException("非法 skill 名称: " + name);
        }
        Path resolved = skillsDirectory.resolve(name);
        if (!resolved.startsWith(skillsDirectory)) {
            throw new IllegalArgumentException("非法 skill 名称: " + name);
        }
    }
}
