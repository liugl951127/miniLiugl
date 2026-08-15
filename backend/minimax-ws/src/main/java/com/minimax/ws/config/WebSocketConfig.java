package com.minimax.ws.config;

import com.minimax.ws.handler.StreamGatewayHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final StreamGatewayHandler streamHandler;
    private final com.minimax.ws.handler.BidirectionalStreamHandler bidirectionalHandler;
    private final com.minimax.ws.handler.CollabWebSocketHandler collabHandler;

    /** V6.8.2: 从环境变量解析 WebSocket 允许的 CORS 源 */
    private String[] wsAllowedOrigins() {
        String env = System.getenv("CORS_ORIGINS");
        if (env != null && !env.isBlank()) {
            return env.trim().split(",");
        }
        return new String[]{ "http://localhost:*", "http://127.0.0.1:*" };
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = wsAllowedOrigins();
        // V4.2: 单向流
        registry.addHandler(streamHandler, "/ws/stream")
                .setAllowedOriginPatterns(origins);
        // V5.19: 双向流 (新)
        registry.addHandler(bidirectionalHandler, "/ws/bidi")
                .setAllowedOriginPatterns(origins);
        // V2.8.7: 实时协作
        registry.addHandler(collabHandler, "/ws/collab")
                .setAllowedOriginPatterns(origins);
        // V6.3+: 通知推送
        registry.addHandler(streamHandler, "/ws/notifications")
                .setAllowedOriginPatterns(origins);
    }
}
