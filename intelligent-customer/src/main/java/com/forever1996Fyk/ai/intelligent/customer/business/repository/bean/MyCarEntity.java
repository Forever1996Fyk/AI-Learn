package com.forever1996Fyk.ai.intelligent.customer.business.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 我的车辆信息表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-08
 */
@Getter
@Setter
@ToString
@TableName("my_car")
public class MyCarEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车辆唯一标识
     */
    private String carId;

    /**
     * 用户ID，标识车辆归属
     */
    private String userId;

    /**
     * 关联的车型信息ID
     */
    private String carInfoId;

    /**
     * 车辆昵称（车主自定义名称）
     */
    private String nickname;

    /**
     * 车辆全称（如：Tesla Model 3 2025焕新版）
     */
    private String fullName;

    /**
     * 车辆图片URL
     */
    private String imageUrl;

    /**
     * 关联的购车订单ID
     */
    private String orderId;

    /**
     * 车牌号
     */
    private String plateNumber;

    /**
     * 车辆颜色（具体颜色，如：珍珠白、深海蓝等）
     */
    private String color;

    /**
     * 车辆识别代号(VIN码)
     */
    private String vin;

    /**
     * 发动机号
     */
    private String engineNumber;

    /**
     * 购买日期
     */
    private Date purchaseDate;

    /**
     * 购买价格
     */
    private BigDecimal purchasePrice;

    /**
     * 行驶里程(公里)
     */
    private Integer mileage;

    /**
     * 注册日期
     */
    private Date registerDate;

    /**
     * 保险到期日
     */
    private Date insuranceExpireDate;

    /**
     * 年检到期日
     */
    private Date inspectionExpireDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
