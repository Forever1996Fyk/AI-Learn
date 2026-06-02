package com.forever1996Fyk.ai.intelligent.customer.document.enums;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/2 22:46
 **/
public enum DocumentStatus {

    /**
     * 初始状态
     */
    INIT,
    /**
     * 上传完成
     */
    UPLOADED,
    /**
     * 转换中
     */
    CONVERTING,
    /**
     * 转换完成
     */
    CONVERTED,
    /**
     * 分块完成
     */
    CHUNKED,
    /**
     * 向量存储完成
     */
    VECTOR_STORED,
    /**
     * 存储完成（不需要向量存储的使用这个状态）
     */
    STORED
    ;
}
