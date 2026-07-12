package com.forever1996Fyk.ai.intelligent.customer.business.repository.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 车型信息表
 * </p>
 *
 * @author MichaelKai
 * @since 2026-07-08
 */
@Getter
@Setter
@ToString
@TableName("car_info")
public class CarInfoEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车型唯一标识
     */
    private String infoId;

    /**
     * 品牌（如：Tesla、比亚迪、宝马等）
     */
    private String brand;

    /**
     * 型号名称（如：Model 3、汉、3系等）
     */
    private String modelName;

    /**
     * 年款（如：2025、2024等）
     */
    private Integer modelYear;

    /**
     * 版本描述（如：焕新版、长续航版、运动版等）
     */
    @Version
    private String version;

    /**
     * 全称（如：Tesla Model 3 2025焕新版）
     */
    private String fullName;

    /**
     * 车辆类型：轿车/SUV/MPV/跑车/皮卡等
     */
    private String vehicleType;

    /**
     * 燃油类型：汽油/柴油/电动/混动/氢能源等
     */
    private String fuelType;

    /**
     * 座位数
     */
    private Integer seatCount;

    /**
     * 排量（L），燃油车使用
     */
    private BigDecimal displacement;

    /**
     * 电机功率（kW），电动车使用
     */
    private BigDecimal motorPower;

    /**
     * 续航里程（km），电动车使用
     */
    private Integer rangeKm;

    /**
     * 官方指导价（万元）
     */
    private BigDecimal guidePrice;

    /**
     * 车身颜色选项（多个颜色用逗号分隔）
     */
    private String colorOptions;

    /**
     * 车身尺寸（长x宽x高，单位mm）
     */
    private String dimensions;

    /**
     * 轴距（mm）
     */
    private Integer wheelbase;

    /**
     * 生产厂商
     */
    private String manufacturer;

    /**
     * 车型状态：在售/停售/即将上市
     */
    private String status;

    /**
     * 车型图片URL
     */
    private String imageUrl;

    /**
     * 车型描述
     */
    private String description;

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
