package com.example.top_hog_server.config;

import com.example.top_hog_server.handler.GameWebSocketHandler; // 新创建的 Handler
import com.example.top_hog_server.handler.UserHandshakeInterceptorRaw; // 新创建的 Handshake Interceptor
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // 启用原始 WebSocket 支持
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private GameWebSocketHandler gameWebSocketHandler; // 注入您将创建的 WebSocket 处理器

    @Autowired
    private UserHandshakeInterceptorRaw userHandshakeInterceptorRaw; // 注入您将创建的握手拦截器

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws-game") // 定义新的 WebSocket 端点
                .addInterceptors(userHandshakeInterceptorRaw) // 添加握手拦截器
                .setAllowedOriginPatterns("*"); // 允许所有来源
    }

    /**
     * 配置用于处理计划任务的 TaskScheduler Bean，
     * 例如 GameLogicService 中的玩家选择超时。
     * @return TaskScheduler 实例
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Configure pool size as needed
        scheduler.setThreadNamePrefix("game-task-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
