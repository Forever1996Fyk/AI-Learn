package com.forever1996Fyk.ai.intelligent.customer.business.service;

import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.StaffInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 员工信息表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
public interface StaffInfoService extends IService<StaffInfoEntity> {

    /**
     * 根据工号查询员工信息
     *
     * @param empId 工号
     * @return 员工信息
     */
    StaffInfoEntity getByEmpId(String empId);
}
