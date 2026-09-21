package com.forever1996Fyk.ai.agentplus.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * sys_user 用户表实体。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 登录密码（明文存储）
     */
    private String password;

    /**
     * 昵称（展示用）
     */
    private String nickname;

    /**
     * 状态：ACTIVE-正常、DISABLED-禁用
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
