package com.forever1996Fyk.ai.intelligent.customer.business.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.forever1996Fyk.ai.intelligent.customer.business.enums.StaffStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * <p>
 * 员工信息表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
@Getter
@Setter
@ToString
@TableName("staff_info")
public class StaffInfoEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工号
     */
    private String empId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 岗位
     */
    private String job;

    /**
     * 入职时间
     */
    private Date entryTime;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 学历：junior、undergraduate、master、doctor
     */
    private String educationalBackground;

    /**
     * 主管ID
     */
    private Long directorId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 工作职责
     */
    private String duty;

    /**
     * 个性签名
     */
    private String motto;

    /**
     * 头像地址
     */
    private String picUrl;

    /**
     * 状态：ON_JOB-在职、OFF_JOB-已离职
     */
    private StaffStatus status;

    /**
     * 离职时间
     */
    private Date resignationTime;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
