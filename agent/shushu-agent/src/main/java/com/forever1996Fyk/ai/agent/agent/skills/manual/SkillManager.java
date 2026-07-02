package com.forever1996Fyk.ai.agent.agent.skills.manual;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forever1996Fyk.ai.agent.agent.skills.manual.config.SkillConfig;
import com.forever1996Fyk.ai.agent.agent.skills.manual.exception.SkillLoadingException;
import com.forever1996Fyk.ai.agent.agent.skills.manual.model.SkillMetadata;
import com.forever1996Fyk.ai.agent.agent.skills.manual.registry.FileSystemSkillRegistry;
import com.forever1996Fyk.ai.agent.agent.skills.manual.registry.SkillRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/2 22:50
 **/
public class SkillManager {
    private static final Logger log = LoggerFactory.getLogger(SkillManager.class);

    public static final String READ_SKILL_TOOL_NAME = "read_skill";
    private final SkillConfig config;
    private final ObjectMapper objectMapper;
    private final SkillRegistry registry;
    private final Function<List<SkillMetadata>, String> promptFormatter;

    private SkillManager(SkillConfig config) {
        this.config = config;
        this.registry = buildSkillRegistry(config);
        this.promptFormatter = config.getPromptFormatter() != null
                ? config.getPromptFormatter()
                : SkillPromptFormatter::format;
        this.objectMapper = new ObjectMapper();
    }

    private static SkillRegistry buildSkillRegistry(SkillConfig config) {
        FileSystemSkillRegistry.Builder builder = FileSystemSkillRegistry.builder();
        for (var dirPath : config.getDirectories()) {
            builder.addDirectory(dirPath);
        }
        return builder.autoReload(config.isAutoReload()).build();
    }

    public static SkillManager create(SkillConfig config) {
        if (config == null) {
            return null;
        }
        return new SkillManager(config);
    }

    public String formatPrompt() {
        List<SkillMetadata> skills = getSkills();
        if (CollectionUtils.isEmpty(skills)) {
            return "";
        }
        return promptFormatter.apply(skills);
    }


    public int getSkillCount() {
        return registry.size();
    }

    public List<SkillMetadata> getSkills() {
        try {
            return new ArrayList<>(registry.listAll());
        } catch (SkillLoadingException e) {
            log.error("Failed to load skills", e);
            return List.of();
        }
    }

    public SkillRegistry getRegistry() {
        return registry;
    }
}
