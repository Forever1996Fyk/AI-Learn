package com.forever1996Fyk.ai.intelligent.customer.business.service;

import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.CarInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 车型信息表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-08
 */
public interface CarInfoService extends IService<CarInfoEntity> {

    /**
     * 根据品牌获取车型信息
     *
     * @param brand 品牌
     * @return 车型信息
     */
    List<CarInfoEntity> getCarInfoByBrand(String brand);
}
