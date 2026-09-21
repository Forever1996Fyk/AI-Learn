package com.forever1996Fyk.ai.agentplus.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * user_profile 用户档案表实体（SELF 数据范围过滤列）。
 */
@Data
@TableName("user_profile")
public class SysUserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联 sys_user.id（SELF 过滤列）
     */
    private Long userId;

    private String realName;

    private String idCard;

    private String homeAddress;

    private Integer age;

    private String education;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
