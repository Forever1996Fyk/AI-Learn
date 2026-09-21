package com.forever1996Fyk.ai.agentplus.sys.dto;

import com.forever1996Fyk.ai.agentplus.auth.dto.DeptSimpleVO;
import com.forever1996Fyk.ai.agentplus.auth.dto.RoleSimpleVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户列表项（含角色/部门简要信息）。
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    /**
     * 登录密码（明文，编辑时回显用）
     */
    private String password;

    private String nickname;

    private String status;

    private LocalDateTime createdAt;

    private List<RoleSimpleVO> roles = new ArrayList<>();

    private List<DeptSimpleVO> depts = new ArrayList<>();

    /**
     * 用户档案（user_profile），可能为 null
     */
    private UserProfileVO profile;
}
