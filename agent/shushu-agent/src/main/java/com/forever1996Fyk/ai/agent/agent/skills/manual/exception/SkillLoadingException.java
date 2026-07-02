package com.forever1996Fyk.ai.agent.agent.skills.manual.exception;

import java.io.IOException;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/2 22:58
 **/
public class SkillLoadingException extends RuntimeException {
    private final String skillName;

    public SkillLoadingException(String message) {
        super(message);
        this.skillName = null;
    }

    public SkillLoadingException(String message, Throwable cause) {
        super(message, cause);
        this.skillName = null;
    }

    public SkillLoadingException(String skillName, String message) {
        super(String.format("[%s] %s", skillName, message));
        this.skillName = skillName;
    }

    public SkillLoadingException(String skillName, String message, Throwable cause) {
        super(String.format("[%s] %s", skillName, message), cause);
        this.skillName = skillName;
    }

    public static SkillLoadingException ioException(String skillName, String path, IOException cause) {
        return new SkillLoadingException(skillName, "Failed to read skill file: " + path, cause);
    }

    public static SkillLoadingException parseException(String skillName, String path, Throwable cause) {
        return new SkillLoadingException(skillName, "Failed to parse skill file: " + path, cause);
    }

    public static SkillLoadingException notFound(String skillName) {
        return new SkillLoadingException(skillName, "Skill not found: " + skillName);
    }

    public String getSkillName() {
        return skillName;
    }
}
