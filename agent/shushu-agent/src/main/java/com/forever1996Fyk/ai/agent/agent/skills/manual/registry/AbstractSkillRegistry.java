package com.forever1996Fyk.ai.agent.agent.skills.manual.registry;

import com.forever1996Fyk.ai.agent.agent.skills.manual.exception.SkillLoadingException;
import com.forever1996Fyk.ai.agent.agent.skills.manual.model.SkillMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/2 22:59
 **/
public abstract class AbstractSkillRegistry implements SkillRegistry {
    private static final Logger log = LoggerFactory.getLogger(AbstractSkillRegistry.class);

    protected final Map<String, SkillMetadata> metadataCache = new ConcurrentHashMap<>();
    protected final Map<String, String> contentCache = new ConcurrentHashMap<>();
    protected volatile boolean loaded = false;

    private final boolean cacheEnabled;

    protected AbstractSkillRegistry() {
        this(true);
    }

    protected AbstractSkillRegistry(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    @Override
    public List<SkillMetadata> listAll() throws SkillLoadingException {
        ensureLoaded();
        return List.copyOf(metadataCache.values());
    }

    @Override
    public SkillMetadata get(String name) throws SkillLoadingException {
        ensureLoaded();
        return metadataCache.get(name);
    }

    @Override
    public boolean contains(String name) {
        try {
            ensureLoaded();
        } catch (SkillLoadingException e) {
            log.error("Failed to load skills", e);
            return false;
        }
        return metadataCache.containsKey(name);
    }

    @Override
    public int size() {
        try {
            ensureLoaded();
        } catch (SkillLoadingException e) {
            log.error("Failed to load skills", e);
            return 0;
        }
        return metadataCache.size();
    }

    @Override
    public String readSkillContent(String name) throws SkillLoadingException {
        if (cacheEnabled && contentCache.containsKey(name)) {
            return contentCache.get(name);
        }
        ensureLoaded();

        SkillMetadata metadata = metadataCache.get(name);
        if (metadata == null) {
            throw SkillLoadingException.notFound(name);
        }

        String content = loadContent(metadata);

        if (cacheEnabled) {
            contentCache.put(name, content);
        }
        return content;
    }

    @Override
    public void clearCache() {
        metadataCache.clear();
        contentCache.clear();
        loaded = false;
        log.debug("Skill registry cache cleared");
    }

    protected void ensureLoaded() throws SkillLoadingException {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    loadSkills();
                    loaded = true;
                    log.debug("Skills loaded: {} skills", metadataCache.size());
                }
            }
        }
    }

    /**
     * Load skills from the source.
     *
     * 子类实现可以从File中读取，也可以从数据库等其他地方获取
     */
    protected abstract void loadSkills() throws SkillLoadingException;

    /**
     * Load skill content from the source.
     */
    protected abstract String loadContent(SkillMetadata metadata) throws SkillLoadingException;
}
