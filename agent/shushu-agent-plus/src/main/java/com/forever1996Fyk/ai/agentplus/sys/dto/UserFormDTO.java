package com.forever1996Fyk.ai.agentplus.sys.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户新增/编辑表单。
 *
 * - 新增：username/password 必填，password 明文入库
 * - 编辑：username 不能改，password 必填（明文回显，可直接修改）
 */
@Data
public class UserFormDTO {

    /**
     * 登录用户名（新增时必填，编辑时忽略）
     */
    private String username;

    /**
     * 密码明文（新增/编辑均必填，直接入库）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 状态：ACTIVE / DISABLED
     * 新增时若为空，后端默认 ACTIVE；编辑时若为空，则不修改原状态（由专门的 toggle 接口切换）。
     */
    private String status;

    /**
     * 角色 ID 列表（会同步到 sys_user_role，先删后插）
     */
    private List<Long> roleIds = new ArrayList<>();

    /**
     * 部门 ID 列表（会同步到 sys_user_dept，先删后插）
     */
    private List<Long> deptIds = new ArrayList<>();

    /**
     * 用户档案（user_profile），非空时按 userId upsert
     */
    private UserProfileVO profile;
}
