package com.forever1996Fyk.ai.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/28 22:43
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slide {
    /**
     * 页面类型
     */
    private String pageType;

    /**
     * 页面描述
     */
    private String pageDesc;

    /**
     * 页面索引（模板页码）
     */
    private Integer templatePageIndex;

    /**
     * 页面数据（字段名 -> 字段数据）
     */
    private Map<String, FieldData> data;
}
