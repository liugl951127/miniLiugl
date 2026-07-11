package com.minimax.ws.config;

import com.minimax.ws.handler.StreamGatewayHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final StreamGatewayHandler streamHandler;
    private final com.minimax.ws.handler.BidirectionalStreamHandler bidirectionalHandler;
    private final com.minimax.ws.handler.CollabWebSocketHandler collabHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // V4.2: 单向流
        registry.addHandler(streamHandler, "/ws/stream")
                .setAllowedOriginPatterns("*");
        // V5.19: 双向流 (新)
        registry.addHandler(bidirectionalHandler, "/ws/bidi")
                .setAllowedOriginPatterns("*");
        // V2.8.7: 实时协作
        registry.addHandler(collabHandler, "/ws/collab")
                .setAllowedOriginPatterns("*");
    }
}
