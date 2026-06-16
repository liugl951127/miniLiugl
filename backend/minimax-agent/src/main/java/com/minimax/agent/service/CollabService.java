package com.minimax.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * V2.3: 实时协作服务 (无 WebSocketHandler 部分, 纯数据层)
 *
 * 数据结构:
 *   rooms: sessionId -> List<WebSocketSession>
 *   userMap: WebSocketSession.id -> userId
 *   history: sessionId -> 消息历史 (最近 100)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabService {

    private final ObjectMapper json = new ObjectMapper();

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> userMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Map<String, Object>>> history = new ConcurrentHashMap<>();

    public void onJoin(WebSocketSession ws) {
        String sessionId = extractSessionId(ws);
        Long userId = extractUserId(ws);
        if (sessionId == null) {
            close(ws, CloseStatus.BAD_DATA.withReason("missing sessionId"));
            return;
        }
        rooms.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(ws);
        userMap.put(ws.getId(), userId == null ? 0L : userId);
        broadcast(sessionId, Map.of("type", "join", "userId", userId, "users", usersInRoom(sessionId)));
        log.info("Collab 接入: session={} user={} 总在线={}", sessionId, userId, rooms.get(sessionId).size());
    }

    public void onMessage(WebSocketSession ws, String payload) {
        String sessionId = extractSessionId(ws);
        if (sessionId == null) return;
        Map<String, Object> msg;
        try {
            msg = json.readValue(payload, Map.class);
        } catch (Exception e) {
            return;
        }
        String type = (String) msg.get("type");
        Long userId = userMap.getOrDefault(ws.getId(), 0L);

        switch (type) {
            case "msg" -> {
                Map<String, Object> broadcast = new HashMap<>();
                broadcast.put("type", "msg");
                broadcast.put("userId", userId);
                broadcast.put("content", msg.get("content"));
                broadcast.put("ts", System.currentTimeMillis());
                broadcast(sessionId, broadcast);
                history.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(broadcast);
            }
            case "typing" -> broadcast(sessionId, Map.of(
                    "type", "typing", "userId", userId, "ts", System.currentTimeMillis()));
            case "cursor" -> broadcast(sessionId, Map.of(
                    "type", "cursor", "userId", userId,
                    "pos", msg.get("pos"), "ts", System.currentTimeMillis()));
            case "edit" -> {
                Map<String, Object> broadcast = new HashMap<>();
                broadcast.put("type", "edit");
                broadcast.put("userId", userId);
                broadcast.put("content", msg.get("content"));
                broadcast(sessionId, broadcast);
            }
            case "ping" -> {
                try { ws.sendMessage(new TextMessage("{\"type\":\"pong\"}")); } catch (Exception ignored) {}
            }
            default -> log.debug("未知类型: {}", type);
        }
    }

    public void onClose(WebSocketSession ws) {
        String sessionId = extractSessionId(ws);
        Long userId = userMap.remove(ws.getId());
        if (sessionId != null && rooms.containsKey(sessionId)) {
            rooms.get(sessionId).remove(ws);
            if (rooms.get(sessionId).isEmpty()) {
                rooms.remove(sessionId);
            } else {
                broadcast(sessionId, Map.of("type", "leave", "userId", userId, "users", usersInRoom(sessionId)));
            }
        }
    }

    public List<Map<String, Object>> getHistory(String sessionId, int limit) {
        if (limit <= 0 || limit > 200) limit = 50;
        List<Map<String, Object>> h = history.get(sessionId);
        if (h == null) return List.of();
        int from = Math.max(0, h.size() - limit);
        return h.subList(from, h.size());
    }

    public int roomSize(String sessionId) {
        var r = rooms.get(sessionId);
        return r == null ? 0 : r.size();
    }

    public Map<String, Integer> allRoomStats() {
        Map<String, Integer> m = new HashMap<>();
        rooms.forEach((k, v) -> m.put(k, v.size()));
        return m;
    }

    private void broadcast(String sessionId, Map<String, Object> msg) {
        var room = rooms.get(sessionId);
        if (room == null) return;
        String json;
        try {
            json = this.json.writeValueAsString(msg);
        } catch (Exception e) {
            return;
        }
        TextMessage tm = new TextMessage(json);
        for (WebSocketSession ws : room) {
            if (ws.isOpen()) {
                try { ws.sendMessage(tm); } catch (Exception ignored) {}
            }
        }
    }

    private List<Long> usersInRoom(String sessionId) {
        var room = rooms.get(sessionId);
        if (room == null) return List.of();
        List<Long> users = new ArrayList<>();
        for (WebSocketSession ws : room) {
            Long u = userMap.getOrDefault(ws.getId(), 0L);
            if (u > 0 && !users.contains(u)) users.add(u);
        }
        return users;
    }

    private String extractSessionId(WebSocketSession ws) {
        String path = ws.getUri() == null ? "" : ws.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 4 && "collab".equals(parts[2])) return parts[3];
        return null;
    }

    private Long extractUserId(WebSocketSession ws) {
        if (ws.getUri() == null) return null;
        String q = ws.getUri().getQuery();
        if (q == null) return null;
        for (String kv : q.split("&")) {
            if (kv.startsWith("userId=")) {
                try { return Long.parseLong(kv.substring(7)); } catch (Exception e) { return null; }
            }
        }
        return null;
    }

    private void close(WebSocketSession ws, CloseStatus status) {
        try { ws.close(status); } catch (Exception ignored) {}
    }
}
