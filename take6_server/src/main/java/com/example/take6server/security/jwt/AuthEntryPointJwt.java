package com.example.take6server.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        logger.error("未授权错误: {}", authException.getMessage());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 使用手动构建 JSON 字符串，以避免依赖 Jackson 等库（尽管项目中有）
        // 这里返回标准 ApiResponse 格式的 JSON
        // code: 401 (来自 ErrorCode.UNAUTHORIZED)
        response.getWriter().write("{\"code\":401,\"message\":\"错误: 未授权 - " + authException.getMessage() + "\",\"data\":null}");
    }
}
