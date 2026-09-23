package com.forever1996Fyk.ai.agentplus.skill;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @program: AI-Learn
 * @description:
 * Skill 内容存储策略（SKILL.md + 资源文件的存取与同步）。
 * <p>
 * 两套实现，配置 deploy.mode 切换：
 * <ul>
 *   <li>standalone（默认）：本地目录为权威源，上传落本地、定时把目录同步进 DB</li>
 *   <li>cluster：DB + MinIO 为权威源，上传落 MinIO，各节点定时按表从 MinIO 拉取到本地</li>
 * </ul>
 * @author: YuKai Fan
 * @create: 2026/9/21 16:59
 **/
public interface SkillContentStore {

    /**
     * 同步（启动时 + 定时）：localFs 实现为目录 → DB 入库；minio 实现为 DB → 本地拉取。
     */
    void sync();

    /**
     * 上传时存储 skill 内容（skillSourceDir 已解压并校验出 SKILL.md）。
     */
    void save(String name, Path skillSourceDir) throws IOException;

    /**
     * DB 里 skill_path 存什么：localFs 返回本地目录，minio 返回 MinIO objectKey。
     */
    String storedPath(String name);

    /**
     *  删除 skill 内容。
     *
     * @param name  技能名称
     */
    void delete(String name) throws IOException;

    /**
     * 内容变更后的通知（DB 写完后调用）：minio 实现广播一条 pub/sub，
     * 各节点收到后立即触发 sync，不必等定时周期。
     */
    default void notifyChanged() {
    }
}

