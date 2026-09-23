package com.forever1996Fyk.ai.agentplus.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.forever1996Fyk.ai.agentplus.domain.entity.AgentxSkill;
import com.forever1996Fyk.ai.agentplus.mapper.AgentxSkillMapper;
import com.forever1996Fyk.ai.agentplus.util.FrontmatterUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/22 17:58
 **/
@Slf4j
@Component
@ConditionalOnProperty(name = "deploy.mode", havingValue = "standalone", matchIfMissing = true)
public class LocalFsSkillContentStore implements SkillContentStore {

    private static final String SKILL_FILE = "SKILL.md";

    private final Path skillsDirectory;
    private final AgentxSkillMapper skillMapper;

    public LocalFsSkillContentStore(@Value("${skills.directory}") String skillsDirectory,
                                    AgentxSkillMapper skillMapper) {
        this.skillsDirectory = Path.of(skillsDirectory);
        this.skillMapper = skillMapper;
    }

    @Override
    public void sync() {
        if (!Files.isDirectory(skillsDirectory)) {
            log.warn("[SkillManager] skills 目录不存在: {}", skillsDirectory.toAbsolutePath());
            return;
        }

        List<String> fsNames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skillsDirectory, Files::isDirectory)) {
            for (Path dir : stream) {
                if (Files.isRegularFile(dir.resolve(SKILL_FILE))) {
                    fsNames.add(dir.getFileName().toString());
                }
            }
        } catch (IOException e) {
            log.warn("[SkillManager] 扫描 skills 目录失败: {}", e.toString());
        }

        List<AgentxSkill> dbSkills = skillMapper.selectList(null);
        List<String> dbNames = dbSkills.stream().map(AgentxSkill::getName).toList();

        // 收集需要新增和更新的记录
        List<AgentxSkill> toInsert = new ArrayList<>();
        List<LambdaUpdateWrapper<AgentxSkill>> toUpdate = new ArrayList<>();

        for (String name : fsNames) {
            String absPath = skillsDirectory.resolve(name).toAbsolutePath().toString();
            String description = FrontmatterUtils.extractFromFile(
                    skillsDirectory.resolve(name).resolve(SKILL_FILE), "description");
            if (description == null) {
                description = "";
            }
            if (!dbNames.contains(name)) {
                AgentxSkill entity = new AgentxSkill();
                entity.setName(name);
                entity.setSkillPath(absPath);
                entity.setDescription(description);
                entity.setEnabled(1);
                entity.setFileName(null);
                toInsert.add(entity);
            } else {
                AgentxSkill existing = dbSkills.stream()
                        .filter(s -> s.getName().equals(name)).findFirst().orElse(null);
                if (existing != null) {
                    boolean descChanged = !description.equals(existing.getDescription());
                    boolean pathChanged = !absPath.equals(existing.getSkillPath());
                    if (descChanged || pathChanged) {
                        LambdaUpdateWrapper<AgentxSkill> uw = new LambdaUpdateWrapper<AgentxSkill>()
                                .eq(AgentxSkill::getName, name);
                        if (descChanged) {
                            uw.set(AgentxSkill::getDescription, description);
                        }
                        if (pathChanged) {
                            uw.set(AgentxSkill::getSkillPath, absPath);
                        }
                        toUpdate.add(uw);
                    }
                }
            }
        }

        // 批量插入新 skill
        if (!toInsert.isEmpty()) {
            skillMapper.insert(toInsert);
            log.info("[SkillManager] 批量入库 {} 条存量 skill (enabled=1)", toInsert.size());
        }

        // 批量更新 skill
        for (LambdaUpdateWrapper<AgentxSkill> uw : toUpdate) {
            skillMapper.update(null, uw);
        }
        if (!toUpdate.isEmpty()) {
            log.info("[SkillManager] 批量更新 {} 条 skill", toUpdate.size());
        }

        // 批量删除孤儿 DB 记录（目录不存在）
        List<String> orphanNames = dbSkills.stream()
                .map(AgentxSkill::getName)
                .filter(name -> !fsNames.contains(name))
                .toList();
        if (!orphanNames.isEmpty()) {
            skillMapper.delete(new LambdaQueryWrapper<AgentxSkill>()
                    .in(AgentxSkill::getName, orphanNames));
            log.warn("[SkillManager] 批量清理 {} 条孤儿 DB 记录（目录不存在）", orphanNames.size());
        }
    }

    @Override
    public void save(String name, Path skillSourceDir) throws IOException {
        Path targetDir = skillsDirectory.resolve(name);
        if (Files.isDirectory(targetDir)) {
            FileSystemUtils.deleteRecursively(targetDir);
        }
        FileSystemUtils.copyRecursively(skillSourceDir.toFile(), targetDir.toFile());
    }

    @Override
    public String storedPath(String name) {
        return skillsDirectory.resolve(name).toAbsolutePath().toString();
    }

    @Override
    public void delete(String name) throws IOException {
        Path skillDir = skillsDirectory.resolve(name);
        if (Files.isDirectory(skillDir)) {
            FileSystemUtils.deleteRecursively(skillDir);
        }
    }
}
