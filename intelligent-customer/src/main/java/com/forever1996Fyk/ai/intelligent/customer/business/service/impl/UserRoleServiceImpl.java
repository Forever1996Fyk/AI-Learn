package com.forever1996Fyk.ai.intelligent.customer.business.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
import com.forever1996Fyk.ai.intelligent.customer.business.enums.StaffStatus;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.MyCarEntity;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.StaffInfoEntity;
import com.forever1996Fyk.ai.intelligent.customer.business.service.MyCarService;
import com.forever1996Fyk.ai.intelligent.customer.business.service.StaffInfoService;
import com.forever1996Fyk.ai.intelligent.customer.business.service.UserRoleService;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatSource;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.forever1996Fyk.ai.intelligent.customer.auth.service.impl.AuthServiceImpl.STAFF_LOGIN_PREFIX;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/11 22:15
 **/
@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Autowired
    private MyCarService myCarService;
    @Autowired
    private StaffInfoService staffInfoService;

    @Override
    public RoleEnum getUserRole(ChatParam chatParam) {
        if (chatParam.chatSource() == ChatSource.STAFF_DING) {
            return RoleEnum.CUSTOMER_SERVICE;
        }
        //再次查询一下车辆，避免水平权限漏洞
        MyCarEntity myCar = myCarService.getCarByUser(chatParam.intentRecognitionResult().entities().car_id(), chatParam.userId());
        if (myCar != null) {
            return RoleEnum.OWNER;
        }
        StaffInfoEntity staffInfo = null;
        String userId = chatParam.userId();
        if (userId != null && userId.startsWith(STAFF_LOGIN_PREFIX)) {
            String empId = userId.substring(STAFF_LOGIN_PREFIX.length());
            staffInfo = staffInfoService.getByEmpId(empId);
        }
        if (staffInfo != null && staffInfo.getStatus() == StaffStatus.ON_JOB) {
            return RoleEnum.CUSTOMER_SERVICE;
        }

        return RoleEnum.VISITOR;
    }
}
