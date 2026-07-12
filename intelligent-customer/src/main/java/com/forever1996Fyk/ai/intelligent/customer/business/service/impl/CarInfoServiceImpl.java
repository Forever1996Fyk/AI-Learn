package com.forever1996Fyk.ai.intelligent.customer.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.CarInfoEntity;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.mapper.CarInfoMapper;
import com.forever1996Fyk.ai.intelligent.customer.business.service.CarInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 车型信息表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-08
 */
@Service
public class CarInfoServiceImpl extends ServiceImpl<CarInfoMapper, CarInfoEntity> implements CarInfoService {

    @Override
    public List<CarInfoEntity> getCarInfoByBrand(String brand) {
        QueryWrapper<CarInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("brand", brand);
        return this.list(wrapper);
    }
}
