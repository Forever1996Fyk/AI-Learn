package com.forever1996Fyk.ai.agentx.core.tools.toolsearch;

/**
 * @program: AI-Learn
 * @description:
 *
 * 延迟工具注册中心。
 * <p>
 * 管理延迟加载的工具池和搜索配置。由 {@code ReactAgent.Builder} 在 build() 时创建，
 * 生命周期与 ReactAgent 实例相同。
 * <p>
 * 共享不可变状态（跨请求复用）：
 * <ul>
 *   <li>deferredTools — 延迟工具池</li>
 *   <li>catalog — Jieba 分词索引（构建成本约 16ms/50 工具）</li>
 *   <li>config / chatModel</li>
 * </ul>
 * <p>
 * 每次请求通过 {@link #createSession()} 创建独立的 {@link Session}，
 * Session 包含请求级别的 discoveredNames 和 toolSearchCallback，
 * 确保不同请求之间的工具发现状态完全隔离。
 * @author: YuKai Fan
 * @create: 2026/8/31 16:00
 **/
public class DeferredToolRegistry {
}
