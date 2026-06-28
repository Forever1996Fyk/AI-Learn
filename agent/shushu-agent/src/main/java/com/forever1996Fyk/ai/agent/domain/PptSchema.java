package com.forever1996Fyk.ai.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/28 22:44
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PptSchema {

    /**
     * 幻灯片列表
     */
    private List<Slide> slides;
}
