package com.example.take6server.handler;


import com.example.take6server.security.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class UserHandshakeInterceptorRaw implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserHandshakeInterceptorRaw.class);
    public static final String USER_IDENTIFIER_SESSION_KEY = "userIdentifier_websocket_session_attr";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        logger.info("Raw WebSocket Handshake Attempt: URI={}", request.getURI());
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String userIdentifier = null;

            userIdentifier = servletRequest.getServletRequest().getParameter("userIdentifier");

            // 处理可能来自客户端 JS 的显式 "undefined" 字符串
            if ("undefined".equals(userIdentifier)) {
                userIdentifier = null;
            }

            if (userIdentifier != null && !userIdentifier.trim().isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdentifier.trim());
                    attributes.put(USER_IDENTIFIER_SESSION_KEY, userId);
                    logger.info("UserIdentifier '{}' (parsed as Long) added to WebSocket session attributes.", userId);
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse userIdentifier '{}' as Long. Connection might be rejected later.", userIdentifier);
                }
            } else {
                logger.warn("UserIdentifier not found in token or query parameters during WebSocket handshake.");
                // 根据策略，我们可以在这里返回 false。目前允许连接，但稍后逻辑可能会失败。
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception == null) {
            logger.info("Raw WebSocket Handshake Succeeded: URI={}", request.getURI());
        } else {
            logger.error("Raw WebSocket Handshake Failed: URI={}, Exception: {}", request.getURI(), exception.getMessage());
        }
    }
}