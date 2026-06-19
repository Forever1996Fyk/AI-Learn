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
 * 文件元数据表，存储文件基本信息和解析后的内容
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Getter
@Setter
@ToString
@TableName("ai_file_info")
public class AiFileInfoEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件类型（pdf/doc/docx/txt/png/jpg等）
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MinIO中的存储路径
     */
    private String minioPath;

    /**
     * 解析后的纯文本内容
     */
    private String extractedText;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 会话ID（可选，用于关联特定会话）
     */
    private String conversationId;

    /**
     * 文件状态：PENDING/PROCESSING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否向量化
     */
    private Byte embed;
}
