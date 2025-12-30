package com.example.top_hog_server.exception;

/**
 * 资源未找到异常类
 * 当预期资源不存在时抛出，通常会导致HTTP 404响应。
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 构造函数，使用预定义的NOT_FOUND错误码和自定义消息。
     * @param message 错误消息
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message); // 默认使用 NOT_FOUND 错误码
    }

    /**
     * 构造函数，允许指定错误码枚举和自定义消息。
     * @param errorCode 错误码枚举
     * @param message 错误消息
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造函数，使用预定义的错误码枚举（包括其默认消息）。
     * @param errorCode 错误码枚举
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}