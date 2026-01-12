package com.example.top_hog_server.exception;

import lombok.Getter;

/**
 * 错误码枚举类。
 * 定义系统中所有的错误码和对应的错误信息。
 */
@Getter
public enum ErrorCode {

    // 通用错误码
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统错误"),
    BUSINESS_ERROR(1002, "业务错误"),
    PARAM_ERROR(1003, "参数错误"),
    UNAUTHORIZED(401, "未授权或认证失败"),
    FORBIDDEN(1005, "禁止访问"),
    ACCESS_DENIED(403, "拒绝访问"), // Alias or specific code
    NOT_FOUND(1006, "资源不存在"),
    RESOURCE_NOT_FOUND(404, "资源未找到"), // Alias

    // 用户相关错误码 (2000-2999)
    USER_NOT_FOUND(2000, "用户不存在"),
    USERNAME_ALREADY_EXISTS(2001, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(2002, "邮箱已存在"),
    PASSWORD_ERROR(2003, "密码错误"),
    LOGIN_FAILED(2004, "登录失败"), // 例如：用户名或密码错误
    REGISTER_FAILED(2005, "注册失败"),
    USER_DISABLED(2006, "用户已禁用"),
    USER_NOT_VERIFIED(2007, "用户邮箱未验证"), // 新增
    INVALID_VERIFICATION_CODE(2008, "无效或已过期的验证码"), // 新增

    // 任务/备忘录相关错误码 (3000-3999)
    TASK_NOT_FOUND(3000, "任务/备忘录不存在"), // 统一名称
    TASK_CREATE_FAILED(3001, "任务/备忘录创建失败"),
    TASK_UPDATE_FAILED(3002, "任务/备忘录更新失败"),
    TASK_DELETE_FAILED(3003, "任务/备忘录删除失败"),
    CATEGORY_ALREADY_EXISTS(3004, "分类已存在"),

    // 输入验证相关错误码
    INVALID_INPUT(1004, "输入参数无效"),

    // 社交账号相关错误码 (4000-4999)
    SOCIAL_BIND_FAILED(4000, "社交账号绑定失败"),
    SOCIAL_UNBIND_FAILED(4001, "社交账号解绑失败");

    private final int code; // 错误码
    private final String message; // 错误信息

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}