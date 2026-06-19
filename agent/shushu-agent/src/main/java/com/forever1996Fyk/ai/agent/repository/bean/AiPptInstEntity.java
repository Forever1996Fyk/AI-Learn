package com.forever1996Fyk.ai.agent.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * <p>
 * AI PPT生成实例表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Getter
@Setter
@ToString
@TableName("ai_ppt_inst")
public class AiPptInstEntity {

    /**
     * 实例ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 选择的模板code
     */
    private String templateCode;

    /**
     * 状态
     */
    private String status;

    /**
     * 用户原始需求
     */
    private String query;

    /**
     * 需求澄清
     */
    private String requirement;

    /**
     * 搜索信息
     */
    private String searchInfo;

    /**
     * 大纲
     */
    private String outline;

    /**
     * AI生成的PPT规划JSON
     */
    private String pptSchema;

    /**
     * 生成的PPT文件URL
     */
    private String fileUrl;

    /**
     * 失败原因
     */
    private String errorMsg;

    private Date createTime;

    private Date updateTime;
}
