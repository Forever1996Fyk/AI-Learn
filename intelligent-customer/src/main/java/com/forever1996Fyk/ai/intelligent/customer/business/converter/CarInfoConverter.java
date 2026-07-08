package com.forever1996Fyk.ai.intelligent.customer.business.converter;

import com.forever1996Fyk.ai.intelligent.customer.business.vo.CarInfoVO;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.CarInfoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/8 23:41
 **/
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CarInfoConverter {


    CarInfoConverter INSTANCE = Mappers.getMapper(CarInfoConverter.class);

    /**
     * CarInfo转CarInfoVO
     * 字段名称一致，自动映射
     */
    CarInfoVO toVO(CarInfoEntity carInfo);

    /**
     * 批量转换
     */
    List<CarInfoVO> toVOList(List<CarInfoEntity> carInfoList);
}
