package com.forever1996Fyk.ai.intelligent.customer.infra.lock;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 22:20
 **/
public class DistributeLockException extends RuntimeException {

    public DistributeLockException() {
    }

    public DistributeLockException(String message) {
        super(message);
    }

    public DistributeLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistributeLockException(Throwable cause) {
        super(cause);
    }

    public DistributeLockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
