package com.forever1996Fyk.ai.intelligent.customer.auth.dto;

import lombok.Data;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/10 16:27
 **/
@Data
public class StaffLoginDTO {
    /**
     * 工号
     */
    private String empId;

    /**
     * 密码
     */
    private String password;
}
