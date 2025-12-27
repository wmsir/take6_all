package com.example.take6server.exception;

import com.example.take6server.payload.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 统一处理系统中的各类异常，返回标准格式的错误响应。
 * 通过 @ControllerAdvice 注解，此类中的 @ExceptionHandler 方法会应用于所有 @Controller。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class); // 新增日志记录器

    /**
     * 处理自定义的资源未找到异常 (ResourceNotFoundException)。
     * @param e ResourceNotFoundException 实例
     * @return 包含错误信息的 ApiResponse 对象
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody // 确保返回的对象被序列化为响应体
    @ResponseStatus(HttpStatus.NOT_FOUND) // 设置HTTP响应状态码为404
    public ApiResponse<Void> handleResourceNotFoundException(ResourceNotFoundException e) {
        logger.warn("资源未找到: {}", e.getMessage()); // 记录警告级别日志
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理自定义的业务逻辑异常 (BusinessException)。
     * @param e BusinessException 实例
     * @return 包含错误信息的 ApiResponse 对象
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 业务异常通常对应客户端错误，返回400
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: [错误码 {}] {}", e.getCode(), e.getMessage()); // 记录警告级别日志
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理方法参数校验异常 (MethodArgumentNotValidException)。
     * 通常由 @Valid 注解触发。
     * @param e MethodArgumentNotValidException 实例
     * @return 包含具体参数错误信息的 ApiResponse 对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 参数校验失败是客户端错误，返回400
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();

        String errorMessage = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("参数校验失败: {}", errorMessage); // 记录警告级别日志
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), "参数错误: " + errorMessage);
    }

    /**
     * 处理访问被拒绝异常 (org.springframework.security.access.AccessDeniedException)。
     * 当用户已认证但无权访问特定资源时抛出。
     * @param e AccessDeniedException 实例
     * @return 包含禁止访问错误信息的 ApiResponse 对象
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN) // 用户已认证但无权限，返回403
    public ApiResponse<Void> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        logger.warn("访问被拒绝: {}", e.getMessage()); // 记录警告级别日志
        return ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
    }

    /**
     * 处理认证异常 (org.springframework.security.core.AuthenticationException)。
     * 当用户认证失败时（如用户名密码错误、Token无效等）抛出。
     * @param e AuthenticationException 实例
     * @return 包含未授权错误信息的 ApiResponse 对象
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED) // 用户认证失败，返回401
    public ApiResponse<Void> handleAuthenticationException(org.springframework.security.core.AuthenticationException e) {
        logger.warn("认证失败: {}", e.getMessage()); // 记录警告级别日志
        // e.getMessage() 可能包含敏感信息，生产环境考虑使用通用错误信息
        return ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), "认证失败: " + e.getMessage());
    }

    /**
     * 处理所有其他未被特定 @ExceptionHandler 捕获的系统级异常。
     * @param e Exception 实例
     * @return 包含系统错误信息的 ApiResponse 对象
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 未知系统错误，返回500
    public ApiResponse<Void> handleException(Exception e) {
        logger.error("未处理的系统异常: ", e); // 记录错误级别日志，包含堆栈信息
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统发生未知错误，请稍后重试或联系管理员。");
    }
}