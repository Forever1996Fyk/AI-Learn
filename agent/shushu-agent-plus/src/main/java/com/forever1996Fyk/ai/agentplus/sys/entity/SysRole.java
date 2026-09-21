package com.forever1996Fyk.ai.agentplus.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * sys_role 角色表实体。
 *
 * data_scope 字段为后续 data-agent 数据权限预留，本次不接入。
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 角色代码（用于权限判断，如 admin）
     */
    private String code;

    /**
     * 角色名称（用于展示，如 系统管理员）
     */
    private String name;

    /**
     * 数据范围：ALL-全部、DEPT_AND_SUB-本部门及子部门、DEPT-仅本部门
     */
    private String dataScope;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 状态：ACTIVE-正常、DISABLED-禁用
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
