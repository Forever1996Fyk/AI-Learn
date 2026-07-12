package com.forever1996Fyk.ai.intelligent.customer.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.forever1996Fyk.ai.intelligent.customer.auth.dto.LoginDTO;
import com.forever1996Fyk.ai.intelligent.customer.auth.dto.LoginUserVO;
import com.forever1996Fyk.ai.intelligent.customer.auth.dto.StaffLoginDTO;
import com.forever1996Fyk.ai.intelligent.customer.auth.service.AuthService;
import com.forever1996Fyk.ai.intelligent.customer.common.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/10 16:26
 **/
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public R<LoginUserVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            LoginUserVO user = authService.login(loginDTO);
            return R.ok(user, "登录成功");
        } catch (RuntimeException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 员工登录（工号 + 姓名）
     */
    @PostMapping("/staffLogin")
    public R<LoginUserVO> staffLogin(@RequestBody StaffLoginDTO staffLoginDTO) {
        try {
            LoginUserVO user = authService.staffLogin(staffLoginDTO);
            return R.ok(user, "员工登录成功");
        } catch (RuntimeException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/userInfo")
    public R<LoginUserVO> userInfo() {
        try {
            return R.ok(authService.getCurrentUser());
        } catch (RuntimeException e) {
            return R.fail(401, e.getMessage());
        }
    }

    /**
     * 是否已登录
     */
    @GetMapping("/isLogin")
    public R<Boolean> isLogin() {
        return R.ok(StpUtil.isLogin());
    }
}
