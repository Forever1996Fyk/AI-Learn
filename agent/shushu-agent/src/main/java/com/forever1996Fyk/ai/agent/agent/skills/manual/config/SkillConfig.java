package com.forever1996Fyk.ai.agent.agent.skills.manual.config;

import com.forever1996Fyk.ai.agent.agent.skills.manual.model.SkillMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/2 22:46
 **/
public class SkillConfig {
    // skills目录
    private final List<Path> directories;
    // skillMetadata 转换为 提示词的模版方法
    private final Function<List<SkillMetadata>, String> promptFormatter;
    // 是否自动加载
    private final boolean autoReload;

    private SkillConfig(Builder builder) {
        this.directories = List.copyOf(builder.directories);
        this.promptFormatter = builder.promptFormatter;
        this.autoReload = builder.autoReload;
    }

    public static Builder builder() {
        return new Builder();
    }


    public List<Path> getDirectories() {
        return directories;
    }

    public Function<List<SkillMetadata>, String> getPromptFormatter() {
        return promptFormatter;
    }

    public boolean isAutoReload() {
        return autoReload;
    }

    public static class Builder {
        private final List<Path> directories = new ArrayList<>();
        private Function<List<SkillMetadata>, String> promptFormatter = null;
        private boolean autoReload = false;

        public Builder addDirectory(String path) {
            return addDirectory(Path.of(path));
        }

        public Builder addDirectory(Path path) {
            Objects.requireNonNull(path, "path must not be null");
            this.directories.add(path);
            return this;
        }

        public Builder promptFormatter(Function<List<SkillMetadata>, String> formatter) {
            this.promptFormatter = formatter;
            return this;
        }

        public Builder autoReload(boolean autoReload) {
            this.autoReload = autoReload;
            return this;
        }

        public SkillConfig build() {
            return new SkillConfig(this);
        }
    }
}
