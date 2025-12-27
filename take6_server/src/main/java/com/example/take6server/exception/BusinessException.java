package com.example.take6server.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务逻辑异常，包含错误码和错误信息
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final int code;
    private final String message;
    
    /**
     * 创建业务异常
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    /**
     * 创建业务异常（使用默认错误码）
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_ERROR, message);
    }
    
    /**
     * 创建业务异常
     * @param errorCode 预定义错误码枚举
     * @param message 错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }
    
    /**
     * 创建业务异常（使用预定义错误码和错误信息）
     * @param errorCode 预定义错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }
}
