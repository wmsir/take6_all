package com.example.top_hog_server.payload.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 电子邮件请求的数据传输对象 (DTO)。
 * 用于请求如发送验证码等操作时传递电子邮件地址。
 */
public class EmailRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式无效")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}