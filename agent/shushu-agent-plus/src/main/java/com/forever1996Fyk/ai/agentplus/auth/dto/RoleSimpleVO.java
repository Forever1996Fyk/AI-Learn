package com.forever1996Fyk.ai.agentplus.auth.dto;

import lombok.Data;

/**
 * 角色简要信息（不含 dataScope/sort/status 等内部字段）。
 */
@Data
public class RoleSimpleVO {

    private Long id;

    private String code;

    private String name;
}
