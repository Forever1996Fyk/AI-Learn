package com.forever1996Fyk.ai.agentplus.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * sys_dept 部门表实体（树形，用 ancestors 存祖先路径）。
 */
@Data
@TableName("sys_dept")
public class SysDept implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门ID（顶级部门为 0）
     */
    private Long parentId;

    /**
     * 祖先路径（逗号分隔的ID链，如 "0,100,101"）
     */
    private String ancestors;

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

    /**
     * 子部门列表（非持久化字段，构建部门树时填充）。
     */
    private transient List<SysDept> children;
}
