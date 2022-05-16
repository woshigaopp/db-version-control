package com.alibaba.ihome.rollback.exception;

public class RollbackInterceptException extends RuntimeException {
    public RollbackInterceptException(String message) {
        super(message);
    }
}
