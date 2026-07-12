package com.forever1996Fyk.ai.intelligent.customer.business.enums;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/11 22:09
 **/
public enum StaffStatus {
    /**
     * 在职
     */
    ON_JOB("ON_JOB", "在职"),
    /**
     * 离职
     */
    OFF_JOB("OFF_JOB", "离职");

    private String code;
    private String desc;

    StaffStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
