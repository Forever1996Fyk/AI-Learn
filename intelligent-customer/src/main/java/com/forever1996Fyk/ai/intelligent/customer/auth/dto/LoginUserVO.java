package com.forever1996Fyk.ai.intelligent.customer.auth.dto;

import lombok.Data;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/10 16:27
 **/
@Data
public class LoginUserVO {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 手机号
     */
    private String phone;

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
     * 用户类型：staff-员工，user-客户
     */
    private String userType;
}
