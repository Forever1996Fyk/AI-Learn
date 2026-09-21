package com.forever1996Fyk.ai.agentplus.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * sys_user_dept 用户-部门关联表实体（联合主键，支持跨部门兼职）。
 */
@Data
@TableName("sys_user_dept")
public class SysUserDept implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long deptId;

    private LocalDateTime createdAt;
}
