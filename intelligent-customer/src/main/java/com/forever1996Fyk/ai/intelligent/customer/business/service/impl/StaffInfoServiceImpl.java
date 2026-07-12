package com.forever1996Fyk.ai.intelligent.customer.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.StaffInfoEntity;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.mapper.StaffInfoMapper;
import com.forever1996Fyk.ai.intelligent.customer.business.service.StaffInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 员工信息表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
@Service
public class StaffInfoServiceImpl extends ServiceImpl<StaffInfoMapper, StaffInfoEntity> implements StaffInfoService {

    @Override
    public StaffInfoEntity getByEmpId(String empId) {
        return this.getOne(new LambdaQueryWrapper<StaffInfoEntity>().eq(StaffInfoEntity::getEmpId, empId));
    }
}
