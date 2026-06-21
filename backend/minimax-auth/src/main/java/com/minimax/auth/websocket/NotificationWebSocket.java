package com.minimax.auth.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.auth.jwt.JwtTokenProvider;
import com.minimax.common.exception.BizException;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 实时通知推送端点。
 *
 * 路径: ws://host/ws/notifications?token=<jwt>
 *
 * 客户端发消息:
 *   { "type": "ping" }    → 心跳响应
 *   { "type": "subscribe" } → 订阅确认
 *
 * 服务端推消息:
 *   { "type": "notification", "data": { ... } }
 *   { "type": "pong" }
 *   { "type": "connected", "userId": 123 }
 */
@Slf4j
@Component
@ServerEndpoint("/ws/notifications")
public class NotificationWebSocket {

    // Spring 注入：每个连接会创建新实例，但这里 static 引用由 spring-ws 统一管理
    private static JwtTokenProvider jwtTokenProvider;
    private static ObjectMapper objectMapper;

    @Autowired
    public void setJwtTokenProvider(JwtTokenProvider jwtTokenProvider) {
        NotificationWebSocket.jwtTokenProvider = jwtTokenProvider;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        NotificationWebSocket.objectMapper = objectMapper;
    }

    // ── session 存储 (userId → sessions) ───────────────────────────────────

    private static final Map<Long, List<Session>> userSessions = new ConcurrentHashMap<>();

    // ── 生命周期 ───────────────────────────────────────────────────────────

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        if (token == null || token.isBlank()) {
            closeQuietly(session, 4001, "missing token");
            return;
        }

        Long userId;
        try {
            userId = jwtTokenProvider.extractUserId(jwtTokenProvider.parse(token));
        } catch (BizException e) {
            closeQuietly(session, 4001, "invalid token");
            return;
        }

        session.getUserProperties().put("userId", userId);
        session.setMaxIdleTimeout(300_000); // 5 min

        userSessions.computeIfAbsent(userId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(session);

        log.info("[WS] Connected: userId={} sessions={}", userId, sessionCount(userId));
        sendText(session, Map.of("type", "connected", "userId", userId));
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (!session.isOpen()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(message, Map.class);
            switch (String.valueOf(msg.getOrDefault("type", ""))) {
                case "ping" -> sendText(session, Map.of("type", "pong"));
                case "subscribe" -> sendText(session, Map.of("type", "subscribed"));
                default -> { /* ignore */ }
            }
        } catch (Exception e) {
            log.debug("[WS] Bad message from {}: {}", userId(session), message);
        }
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = userId(session);
        if (userId != null) {
            removeSession(userId, session);
            log.info("[WS] Disconnected: userId={} remaining={}", userId, sessionCount(userId));
        }
    }

    @OnError
    public void onError(Session session, Throwable err) {
        Long userId = userId(session);
        log.warn("[WS] Error userId={}: {}", userId, err.getMessage());
        if (userId != null) removeSession(userId, session);
    }

    // ── 广播 API ────────────────────────────────────────────────────────────

    /**
     * 向指定用户所有在线终端推送消息。
     */
    public static void sendToUser(Long userId, Object payload) {
        List<Session> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("[WS] No active session for userId={}", userId);
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(
                    "type", "notification",
                    "data", payload
            ));
        } catch (Exception e) {
            log.error("[WS] Serialize error", e);
            return;
        }

        for (Session s : List.copyOf(sessions)) {
            if (!s.isOpen()) {
                removeSession(userId, s);
                continue;
            }
            try {
                s.getBasicRemote().sendText(json);
            } catch (IOException e) {
                log.warn("[WS] Send failed to userId={}: {}", userId, e.getMessage());
                removeSession(userId, s);
            }
        }
    }

    // ── 工具方法 ────────────────────────────────────────────────────────────

    private Long userId(Session session) {
        Object uid = session.getUserProperties().get("userId");
        if (uid instanceof Long) return (Long) uid;
        if (uid instanceof Integer) return ((Integer) uid).longValue();
        return null;
    }

    private int sessionCount(Long userId) {
        List<Session> s = userSessions.get(userId);
        return s == null ? 0 : s.size();
    }

    private static void removeSession(Long userId, Session session) {
        List<Session> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) userSessions.remove(userId);
        }
    }

    private static void closeQuietly(Session session, int code, String reason) {
        try {
            session.close(new CloseReason(
                    CloseReason.CloseCodes.getCloseCode(code), reason));
        } catch (IOException ignored) {}
    }

    private static void sendText(Session session, Object obj) {
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(obj));
        } catch (IOException e) {
            log.warn("[WS] sendText error: {}", e.getMessage());
        }
    }
}