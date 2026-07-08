package com.forever1996Fyk.ai.intelligent.customer.business.converter;
import com.forever1996Fyk.ai.intelligent.customer.business.vo.MyCarVO;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.MyCarEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/8 23:38
 **/
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MyCarConverter {


    MyCarConverter INSTANCE = Mappers.getMapper(MyCarConverter.class);

    /**
     * MyCar转MyCarVO
     * 字段名称一致，自动映射
     */
    MyCarVO toVO(MyCarEntity myCar);

    /**
     * 批量转换
     */
    List<MyCarVO> toVOList(List<MyCarEntity> myCars);
}
