package com.forever1996Fyk.ai.agentplus.sys.dto;

import lombok.Data;

/**
 * 重置密码入参（明文密码直接入库）。
 */
@Data
public class ResetPasswordDTO {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 新密码明文
     */
    private String newPassword;
}
