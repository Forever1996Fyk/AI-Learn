package com.forever1996Fyk.ai.agentplus.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forever1996Fyk.ai.agentplus.domain.entity.AgentxSkill;
import com.forever1996Fyk.ai.agentplus.mapper.AgentxSkillMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * Skills 管理器 — 负责上传校验、启停、删除、启用目录查询（内容存储委托给 SkillContentStore）。
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
                log.warn("[SkillManager] enabled skill 目录不存在: {} → {}", s.getName(), path);
            }
        }
        return dirs;
    }
}
