package com.forever1996Fyk.ai.agentplus.auth.dto;

import com.forever1996Fyk.ai.agentplus.sys.dto.UserProfileVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录用户完整信息（登录成功 + /auth/me 接口返回）。
 *
 * - roles：用户拥有的所有角色（多角色并集）
 * - depts：用户归属的所有部门（支持跨部门兼职）
 * - dataScope：多角色合并后的最大数据范围（ALL/DEPT_AND_SUB/DEPT/SELF）
 * - profile：用户档案（user_profile），可能为 null（老用户/未配置）
 * - token：Sa-Token 当前会话 token，前端可用于 Header/URL 参数传递
 */
@Data
public class LoginUserVO {

    private Long id;

    private String username;

    private String nickname;

    /**
     * 状态：ACTIVE-正常、DISABLED-禁用
     */
    private String status;

    private List<RoleSimpleVO> roles = new ArrayList<>();

    private List<DeptSimpleVO> depts = new ArrayList<>();

    /**
     * 数据范围（多角色取最大值）
     */
    private String dataScope;

    /**
     * 用户档案（user_profile），可能为 null
     */
    private UserProfileVO profile;

    /**
     * Sa-Token 当前会话 token
     */
    private String token;
}
