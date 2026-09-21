package com.forever1996Fyk.ai.agentplus.sys.dto;

import lombok.Data;

/**
 * 用户档案展示对象（不含 dept_id，部门走 sys_user_dept 多对多）。
 */
@Data
public class UserProfileVO {

    private String realName;

    private String idCard;

    private Integer age;

    private String education;

    private String homeAddress;
}
