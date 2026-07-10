package com.forever1996Fyk.ai.intelligent.customer.auth.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.auth.dto.LoginDTO;
import com.forever1996Fyk.ai.intelligent.customer.auth.dto.LoginUserVO;
import com.forever1996Fyk.ai.intelligent.customer.auth.dto.StaffLoginDTO;
import com.forever1996Fyk.ai.intelligent.customer.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/10 16:28
 **/
public class AuthServiceImpl implements AuthService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STAFF_LOGIN_PREFIX = "staff_";
    private static final String STAFF_DEVICE = "staff";

//    @Autowired
//    private UserInfoService userInfoService;
//
//    @Autowired
//    private StaffInfoService staffInfoService;
//
    @Override
    public LoginUserVO login(LoginDTO loginDTO) {
        return null;
    }

    @Override
    public void logout() {

    }

    @Override
    public LoginUserVO getCurrentUser() {
        return null;
    }

    @Override
    public LoginUserVO staffLogin(StaffLoginDTO staffLoginDTO) {
        return null;
    }

    @Override
    public String getCurrentUserId() {
        return "";
    }

    @Override
    public boolean isStaffLogin() {
        return false;
    }
}
