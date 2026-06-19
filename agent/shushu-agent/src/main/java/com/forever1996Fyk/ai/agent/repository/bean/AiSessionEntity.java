package com.forever1996Fyk.ai.agent.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 存储智能体与用户的对话历史，支持会话隔离和记忆功能
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Getter
@Setter
@ToString
@TableName("ai_session")
public class AiSessionEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户问题
     */
    private String question;

    /**
     * AI回复
     */
    private String answer;

    /**
     * 涉及的执行工具名称（逗号分隔）
     */
    private String tools;

    /**
     * 首次响应时间（毫秒）
     */
    private Long firstResponseTime;

    /**
     * 整体回复时间（毫秒）
     */
    private Long totalResponseTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 参考链接
     */
    private String reference;

    /**
     * 智能体类型
     */
    private String agentType;

    /**
     * 思考过程
     */
    private String thinking;

    /**
     * 文件id
     */
    private String fileid;

    /**
     * 推荐问题
     */
    private String recommend;
}
