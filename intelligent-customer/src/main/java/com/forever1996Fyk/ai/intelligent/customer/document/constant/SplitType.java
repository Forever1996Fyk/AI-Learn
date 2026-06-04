package com.forever1996Fyk.ai.intelligent.customer.document.constant;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 09:26
 **/
public enum SplitType {
    /**
     * 按长度切分
     */
    LENGTH,

    /**
     * 按标题切分
     */
    TITLE,

    /**
     * 按正则切分
     */
    REGEX,

    /**
     * 智能切分
     */
    SMART,

    /**
     * 按分隔符切分
     */
    SEPARATOR
    ;
}
