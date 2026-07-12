package com.forever1996Fyk.ai.intelligent.customer.business.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * <p>
 * 客户信息表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
@Getter
@Setter
@ToString
@TableName("user_info")
public class UserInfoEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号（登录账号）
     */
    private String phone;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 姓名
     */
    private String name;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 状态：ACTIVE-正常、FROZEN-冻结
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
