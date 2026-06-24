package com.forever1996Fyk.ai.agent.service;

import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/24 22:58
 **/
public interface PptRenderService {
    /**
     * 调用Python脚本渲染PPT
     *
     * @param inst      PPT实例
     * @param pptSchema PPT Schema JSON
     * @return 生成的PPT文件URL
     * @throws Exception 渲染失败时抛出异常
     */
    String renderPpt(AiPptInstEntity inst, String pptSchema) throws Exception;
}
