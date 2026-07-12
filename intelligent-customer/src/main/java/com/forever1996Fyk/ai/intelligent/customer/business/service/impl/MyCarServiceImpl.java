package com.forever1996Fyk.ai.intelligent.customer.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.MyCarEntity;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.mapper.MyCarMapper;
import com.forever1996Fyk.ai.intelligent.customer.business.service.MyCarService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 我的车辆信息表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-08
 */
@Service
public class MyCarServiceImpl extends ServiceImpl<MyCarMapper, MyCarEntity> implements MyCarService {

    @Override
    public List<MyCarEntity> getCarByUserId(String userId) {
        QueryWrapper<MyCarEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return this.list(wrapper);
    }

    @Override
    public MyCarEntity getCarByUser(String carId, String userId) {
        QueryWrapper<MyCarEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("car_id", carId);
        wrapper.eq("user_id", userId);
        return this.getOne(wrapper);
    }
}
