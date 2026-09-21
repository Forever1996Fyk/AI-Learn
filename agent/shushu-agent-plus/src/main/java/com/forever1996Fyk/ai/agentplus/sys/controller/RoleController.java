package com.forever1996Fyk.ai.agentplus.sys.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.forever1996Fyk.ai.agentplus.common.R;
import com.forever1996Fyk.ai.agentplus.sys.entity.SysRole;
import com.forever1996Fyk.ai.agentplus.sys.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色接口（只读列表，仅 admin 可访问）。
 */
@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
@SaCheckRole("admin")
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色列表（用于用户编辑时选择角色）。
     */
    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(roleService.list());
    }
}
