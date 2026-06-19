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
 * AI PPT模板表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Getter
@Setter
@ToString
@TableName("ai_ppt_template")
public class AiPptTemplateEntity {

    /**
     * 模板ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板唯一编码
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板说明
     */
    private String templateDesc;

    /**
     * 模板结构JSON
     */
    private String templateSchema;

    /**
     * PPT模板文件路径
     */
    private String filePath;

    /**
     * 风格标签：科技,商务,简约
     */
    private String styleTags;

    /**
     * 模板页数
     */
    private Integer slideCount;

    private Date createTime;
}
