package com.forever1996Fyk.ai.agent.agent.skills.manual.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/2 22:46
 **/
public record SkillMetadata(
        String name,
        String description,
        Path skillPath,
        SkillSource source,
        List<String> allowedTools,
        Path skillFile
) {


    public enum SkillSource {
        /**
         * 项目技能目录
         */
        PROJECT,
        /**
         * 用户技能目录
         */
        USER
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private Path skillPath;
        private SkillSource source;
        private List<String> allowedTools;
        private Path skillFile;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder skillPath(Path skillPath) {
            this.skillPath = skillPath;
            return this;
        }

        public Builder source(SkillSource source) {
            this.source = source;
            return this;
        }

        public Builder allowedTools(List<String> allowedTools) {
            this.allowedTools = allowedTools;
            return this;
        }

        public Builder skillFile(Path skillFile) {
            this.skillFile = skillFile;
            return this;
        }

        public SkillMetadata build() {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(description, "description must not be null");
            Objects.requireNonNull(skillPath, "skillPath must not be null");
            Objects.requireNonNull(source, "source must not be null");
            return new SkillMetadata(name, description, skillPath, source, allowedTools, skillFile);
        }
    }


}
