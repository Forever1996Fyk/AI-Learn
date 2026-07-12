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
 * 车辆订单表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-09
 */
@Getter
@Setter
@ToString
@TableName("car_order")
public class CarOrderEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单唯一标识
     */
    private String orderId;

    /**
     * 用户ID，标识订单归属
     */
    private String userId;

    /**
     * 关联的车辆ID
     */
    private String carId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单类型：购买/出售/置换
     */
    private String orderType;

    /**
     * 订单状态：待支付/已支付/已完成/已取消
     */
    private String orderStatus;

    /**
     * 车辆品牌
     */
    private String brand;

    /**
     * 车辆型号
     */
    private String model;

    /**
     * 车辆颜色
     */
    private String color;

    /**
     * 车辆VIN码
     */
    private String vin;

    /**
     * 卖家/经销商名称
     */
    private String sellerName;

    /**
     * 卖家/经销商联系方式
     */
    private String sellerContact;

    /**
     * 车辆价格
     */
    private BigDecimal vehiclePrice;

    /**
     * 购置税
     */
    private BigDecimal purchaseTax;

    /**
     * 保险费用
     */
    private BigDecimal insuranceFee;

    /**
     * 其他费用
     */
    private BigDecimal otherFee;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 实际支付金额
     */
    private BigDecimal actualAmount;

    /**
     * 付款方式：全款/分期
     */
    private String paymentMethod;

    /**
     * 订单日期
     */
    private Date orderDate;

    /**
     * 交付日期
     */
    private Date deliveryDate;

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
