package com.forever1996Fyk.ai.agentplus.sys.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.forever1996Fyk.ai.agentplus.common.R;
import com.forever1996Fyk.ai.agentplus.sys.dto.AssignDeptsDTO;
import com.forever1996Fyk.ai.agentplus.sys.dto.AssignRolesDTO;
import com.forever1996Fyk.ai.agentplus.sys.dto.ResetPasswordDTO;
import com.forever1996Fyk.ai.agentplus.sys.dto.UserFormDTO;
import com.forever1996Fyk.ai.agentplus.sys.dto.UserVO;
import com.forever1996Fyk.ai.agentplus.sys.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（仅 admin 可访问，由 @SaCheckRole + 拦截器双重守卫）。
 */
@Slf4j
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
@SaCheckRole("admin")
public class UserController {

    private final UserService userService;

    /**
     * 分页列表，支持关键字搜索（username/nickname 模糊）。
     */
    @GetMapping("/list")
    public R<IPage<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String keyword) {
        return R.ok(userService.list(page, size, keyword));
    }

    /**
     * 新增用户。
     */
    @PostMapping
    public R<UserVO> create(@RequestBody UserFormDTO form) {
        return R.ok("新增成功", userService.create(form));
    }

    /**
     * 编辑用户。
     */
    @PutMapping("/{id}")
    public R<UserVO> update(@PathVariable Long id, @RequestBody UserFormDTO form) {
        return R.ok("更新成功", userService.update(id, form));
    }

    /**
     * 删除用户（级联删除角色/部门关联）。
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok("删除成功", null);
    }

    /**
     * 给用户分配角色。
     */
    @PutMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return R.ok();
    }

    /**
     * 切换用户启用/禁用状态。
     */
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        userService.changeStatus(id, status);
        return R.ok();
    }

    /**
     * 给用户分配部门。
     */
    @PutMapping("/{id}/depts")
    public R<Void> assignDepts(@PathVariable Long id, @RequestBody AssignDeptsDTO dto) {
        userService.assignDepts(id, dto.getDeptIds());
        return R.ok();
    }

    /**
     * 重置密码。
     */
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto.getUserId(), dto.getNewPassword());
        return R.ok("重置成功", null);
    }
}
