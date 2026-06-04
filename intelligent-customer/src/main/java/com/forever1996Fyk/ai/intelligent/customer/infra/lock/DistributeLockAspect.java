package com.forever1996Fyk.ai.intelligent.customer.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 22:16
 **/
@Slf4j
@Aspect
@Component
// 这里要加 Order，主要是为了解决分布式锁与事物冲突问题。先开启事务，再加锁，然后执行代码，再解锁，最后才提交事务，就会导致，锁释放，但事务未提交，就会出现数据冲突
// 加上 Order 并指定最先执行，会在开启事务前加锁
@Order(Integer.MIN_VALUE)
public class DistributeLockAspect {

    private final RedissonClient redissonClient;

    public DistributeLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(com.forever1996Fyk.ai.intelligent.customer.infra.lock.DistributeLock)")
    public Object process(ProceedingJoinPoint point) throws Throwable {
        Object response = null;
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);
        String key = distributeLock.key();
        if (DistributeLockConstant.NONE_KEY.equals(key)) {
            if (DistributeLockConstant.NONE_KEY.equals(distributeLock.keyExpression())) {
                throw new DistributeLockException("no lock key found ...");
            }
            SpelExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(distributeLock.keyExpression());
            StandardEvaluationContext context = new StandardEvaluationContext();
            // 获取参数值
            Object[] args = point.getArgs();
            // 获取运行时参数名称
            StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();
            String[] parameterNames = discoverer.getParameterNames(method);
            // 将参数绑定到 context 中
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }

            // 解析表达式，获取结果
            key = String.valueOf(expression.getValue(context));
        }

        String scene = distributeLock.scene();

        String lockKey = scene + "#" + key;

        int expireTime = distributeLock.expireTime();
        int waitTime = distributeLock.waitTime();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean lockResult;
            if (waitTime == DistributeLockConstant.DEFAULT_WAIT_TIME) {
                if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {
                    log.info(String.format("lock for key : %s", lockKey));
                    lock.lock();
                } else {
                    log.info(String.format("lock for key : %s with expire time : %s", lockKey, expireTime));
                    lock.lock(expireTime, TimeUnit.MILLISECONDS);
                }
                lockResult = true;
            } else {
                if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {
                    log.info(String.format("lock for key : %s", lockKey));
                    lockResult = lock.tryLock(waitTime,TimeUnit.MILLISECONDS);
                } else {
                    log.info(String.format("lock for key : %s with expire time : %s", lockKey, expireTime));
                    lockResult = lock.tryLock(waitTime, expireTime, TimeUnit.MILLISECONDS);
                }
            }
            if (!lockResult) {
                log.warn(String.format("lock failed for key : %s, expire : %s", lockKey, expireTime));
                throw new DistributeLockException("acquire lock failed... key:" + lockKey);
            }

            log.info(String.format("lock success for key : %s, expire : %s", lockKey, expireTime));
            response = point.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info(String.format("unlock for key : %s", lockKey));
            }
        }
        return response;
    }

}
