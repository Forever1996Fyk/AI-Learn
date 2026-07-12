package com.forever1996Fyk.ai.intelligent.customer.business.service;

import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.MyCarEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 我的车辆信息表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-08
 */
public interface MyCarService extends IService<MyCarEntity> {

    /**
     * 根据用户ID获取车辆信息
     * @param userId
     * @return
     */
    List<MyCarEntity> getCarByUserId(String userId);

    /**
     * 根据车辆ID和用户ID获取车辆信息
     * @param carId
     * @param userId
     * @return
     */
    MyCarEntity getCarByUser(String carId,String userId);
}
