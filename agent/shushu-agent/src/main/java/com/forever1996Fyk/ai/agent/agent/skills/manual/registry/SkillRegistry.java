package com.forever1996Fyk.ai.agent.agent.skills.manual.registry;

import com.forever1996Fyk.ai.agent.agent.skills.manual.exception.SkillLoadingException;
import com.forever1996Fyk.ai.agent.agent.skills.manual.model.SkillMetadata;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/2 22:52
 **/
public interface SkillRegistry {

    List<SkillMetadata> listAll() throws SkillLoadingException;

    SkillMetadata get(String name) throws SkillLoadingException;

    boolean contains(String name);

    int size();

    String readSkillContent(String name) throws SkillLoadingException;

    default void reload() throws SkillLoadingException {
        throw new UnsupportedOperationException("Reload not supported");
    }

    default void clearCache() {
        // 默认不做任何操作
    }
}
