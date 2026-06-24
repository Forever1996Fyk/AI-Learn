package com.forever1996Fyk.ai.agent.service;

import com.forever1996Fyk.ai.agent.enums.ImageProvider;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 23:02
 **/
public interface ImageGenerationService {

    /**
     * 生成图片
     *
     * @param prompt prompt
     * @return 图像URL
     */
    String generateImage(String prompt);

    /**
     * 获取图片
     *
     * @param prompt prompt
     * @param provider 提供商
     * @return 图像URL
     */
    String generateImage(String prompt, ImageProvider provider);
}
