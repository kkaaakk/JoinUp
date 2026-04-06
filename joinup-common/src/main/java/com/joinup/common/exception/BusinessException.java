package com.joinup.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 * 约定所有可预期的业务失败都抛这个异常，再由全局异常处理器统一转成 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
