package com.forever1996Fyk.ai.intelligent.customer.business.service;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.RoleEnum;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/11 22:15
 **/
public interface UserRoleService {

    /**
     * 获取用户角色
     *
     * @param chatParam
     * @return
     */
    RoleEnum getUserRole(ChatParam chatParam);
}
