package com.forever1996Fyk.ai.intelligent.customer.infra.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 22:13
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributeLock {

    /**
     * 锁场景
     *
     * @return String
     */
    String scene();

    /**
     * 加锁的key，优先取key()，如果没有，则取keyExpression()
     *
     * @return String
     */
    String key() default DistributeLockConstant.NONE_KEY;

    /**
     *SPEL表达式
     * <pre>
     *     #id
     *     #insertResult.id
     * </pre>
     * @return String
     */
    String keyExpression() default DistributeLockConstant.NONE_KEY;

    /**
     * 超时时间，毫秒
     * 默认情况下不设置超时时间，会自动续期
     *
     * @return int
     */
    int expireTime() default DistributeLockConstant.DEFAULT_EXPIRE_TIME;

    /**
     * 加锁等待时长，毫秒
     * 默认情况下不设置等待时长，会一直等待直到获取到锁
     *
     * @return int
     */
    int waitTime() default DistributeLockConstant.DEFAULT_WAIT_TIME;
}
