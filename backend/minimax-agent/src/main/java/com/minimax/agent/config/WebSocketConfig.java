package com.minimax.agent.config;

import com.minimax.agent.service.CollabHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import jakarta.annotation.PostConstruct;

/**
 * Agent WebSocket 配置 (V3.5.17+ 标记为 deprecated)
 *
 * <h3>合并说明</h3>
 * 协作 WebSocket (Collab) 功能在 V3.5.17+ 统一由 {@code minimax-ws} 模块提供
 * (端口 8095, 路径 {@code /ws/collab}).
 *
 * <p>本配置保留是因为:
 * <ul>
 *   <li>向下兼容: 老 client 可能连 8088 端口的 /ws/collab/**</li>
 *   <li>agent 内部测试: 某些 agent-to-agent 通信仍走 8088</li>
 * </ul>
 *
 * <p>新功能开发请用 minimax-ws (8095), 路径统一.
 *
 * @deprecated V3.5.17+ 推荐用 {@code minimax-ws} (端口 8095)
 * @author MiniMax
 */
@Deprecated
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final CollabHandler collabHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // V3.5.17+ 标 deprecated, 业务统一走 ws (8095)
        log.warn("[V3.5.17+] agent /ws/collab/** deprecated, use minimax-ws (port 8095) /ws/collab instead");
        registry.addHandler(collabHandler, "/ws/collab/**")
                .setAllowedOriginPatterns("*");
    }
}
