package com.minimax.ws.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.minimax.ws.collab.CollabService;
import com.minimax.ws.entity.CollabMessage;
import com.minimax.ws.entity.CollabParticipant;
import com.minimax.ws.entity.CollabRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 协作 WebSocket 处理器 (V2.8.7 实时协作)
 *
 * <h3>连接路径</h3>
 * <pre>
 *   ws://host/ws/collab?roomId=ABC123&userId=1&username=alice
 * </pre>
 *
 * <h3>服务端 → 客户端</h3>
 * <ul>
 *   <li>{ type: "ROOM_STATE", room, participants }  // 加入时全量同步</li>
 *   <li>{ type: "MESSAGE", ...message }               // 聊天/AI/编辑消息</li>
 *   <li>{ type: "PARTICIPANT_UPDATE", participants }  // 参与者状态变化</li>
 *   <li>{ type: "CURSOR", userId, x, y, selectionId } // 其他用户光标</li>
 *   <li>{ type: "AI_CHUNK", content, finished }       // AI 流式生成</li>
 *   <li>{ type: "ERROR", message }                    // 错误</li>
 * </ul>
 *
 * <h3>客户端 → 服务端</h3>
 * <ul>
 *   <li>{ action: "join" }                       (兼容 query 自动 join)</li>
 *   <li>{ action: "chat", content, clientMsgId }</li>
 *   <li>{ action: "cursor", x, y, selectionId }</li>
 *   <li>{ action: "edit", op, payload }          (CRDT 风格)</li>
 *   <li>{ action: "ai", prompt }                 (触发 AI 协作生成)</li>
 *   <li>{ action: "heartbeat" }</li>
 *   <li>{ action: "leave" }</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollabWebSocketHandler extends TextWebSocketHandler {

    private final CollabService collabService;
    private final ObjectMapper objectMapper;

    // session → 房间+用户元数据
    private final Map<String, SessionContext> sessionContexts = new java.util.concurrent.ConcurrentHashMap<>();

    private static class SessionContext {
        String roomId;
        Long userId;
        String username;
        String nickname;
        String avatar;
        String role;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 从 query 解析 roomId/userId/username
        URI uri = session.getUri();
        Map<String, String> params = parseQuery(uri);
        String roomId = params.get("roomId");
        String userIdStr = params.get("userId");
        String username = params.getOrDefault("username", "anonymous");
        String nickname = params.getOrDefault("nickname", username);
        String avatar = params.get("avatar");

        if (roomId == null || userIdStr == null) {
            session.sendMessage(toJson(Map.of("type", "ERROR", "message", "缺少 roomId 或 userId")));
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            session.sendMessage(toJson(Map.of("type", "ERROR", "message", "userId 格式错误")));
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 2. 加入房间
        boolean ok = collabService.joinRoom(roomId, userId, username, nickname, avatar, "EDITOR");
        if (!ok) {
            session.sendMessage(toJson(Map.of("type", "ERROR", "message", "加入失败 (房间不存在/已满/已关闭)")));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // 3. 记录 session 上下文
        SessionContext ctx = new SessionContext();
        ctx.roomId = roomId;
        ctx.userId = userId;
        ctx.username = username;
        ctx.nickname = nickname;
        ctx.avatar = avatar;
        sessionContexts.put(session.getId(), ctx);

        // 4. 注册到房间 session 列表
        collabService.roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(session);

        // 5. 推送房间完整状态
        CollabRoom room = collabService.getRoom(roomId);
        List<CollabParticipant> participants = collabService.getParticipants(roomId, true);
        ObjectNode stateMsg = objectMapper.createObjectNode();
        stateMsg.put("type", "ROOM_STATE");
        stateMsg.putPOJO("room", room);
        stateMsg.putPOJO("participants", participants);
        stateMsg.put("yourUserId", userId);
        session.sendMessage(new TextMessage(stateMsg.toString()));

        // 6. 广播参与者列表变化
        broadcastToRoom(roomId, Map.of(
            "type", "PARTICIPANT_UPDATE",
            "participants", participants,
            "event", "JOIN",
            "userId", userId,
            "nickname", nickname
        ), userId);

        log.info("[collab] 连接建立 roomId={} userId={} sessionId={}", roomId, userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionContext ctx = sessionContexts.get(session.getId());
        if (ctx == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        JsonNode req;
        try {
            req = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            sendError(session, "消息格式错误: " + e.getMessage());
            return;
        }

        String action = req.path("action").asText("");
        switch (action) {
            case "chat" -> handleChat(session, ctx, req);
            case "cursor" -> handleCursor(session, ctx, req);
            case "edit" -> handleEdit(session, ctx, req);
            case "ai" -> handleAi(session, ctx, req);
            case "heartbeat" -> handleHeartbeat(session, ctx, req);
            case "leave" -> session.close(CloseStatus.NORMAL);
            case "ping" -> session.sendMessage(toJson(Map.of("type", "PONG", "ts", System.currentTimeMillis())));
            default -> sendError(session, "未知 action: " + action);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionContext ctx = sessionContexts.remove(session.getId());
        if (ctx == null) return;

        // 移除 session
        List<WebSocketSession> sessions = collabService.roomSessions.get(ctx.roomId);
        if (sessions != null) {
            sessions.remove(session);
        }

        // 标记用户离开
        collabService.leaveRoom(ctx.roomId, ctx.userId);

        // 广播参与者变化
        try {
            List<CollabParticipant> participants = collabService.getParticipants(ctx.roomId, true);
            broadcastToRoom(ctx.roomId, Map.of(
                "type", "PARTICIPANT_UPDATE",
                "participants", participants,
                "event", "LEAVE",
                "userId", ctx.userId,
                "nickname", ctx.nickname
            ), -1L);
        } catch (Exception e) {
            log.warn("[collab] 广播离开事件失败: {}", e.getMessage());
        }

        log.info("[collab] 连接关闭 roomId={} userId={} status={}", ctx.roomId, ctx.userId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[collab] 传输错误 sessionId={}: {}", session.getId(), exception.getMessage());
    }

    // ============= 消息处理 =============

    private void handleChat(WebSocketSession session, SessionContext ctx, JsonNode req) throws Exception {
        String content = req.path("content").asText("");
        String clientMsgId = req.path("clientMsgId").asText(null);
        if (content.isEmpty()) {
            sendError(session, "聊天内容不能为空");
            return;
        }
        if (content.length() > 2000) {
            sendError(session, "聊天内容超过 2000 字符");
            return;
        }

        CollabMessage msg = collabService.saveMessage(ctx.roomId, ctx.userId, ctx.username, ctx.nickname,
                "CHAT", content, null, clientMsgId);

        broadcastToRoom(ctx.roomId, Map.of(
            "type", "MESSAGE",
            "id", msg.getId(),
            "userId", ctx.userId,
            "username", ctx.username,
            "nickname", ctx.nickname,
            "messageType", "CHAT",
            "content", content,
            "clientMsgId", clientMsgId == null ? "" : clientMsgId,
            "createdAt", msg.getCreatedAt().toString()
        ), -1L);
    }

    private void handleCursor(WebSocketSession session, SessionContext ctx, JsonNode req) {
        int x = req.path("x").asInt(0);
        int y = req.path("y").asInt(0);
        String selectionId = req.path("selectionId").asText(null);
        collabService.heartbeat(ctx.roomId, ctx.userId, x, y, selectionId);

        // 广播给其他用户 (不广播给自己)
        broadcastToRoom(ctx.roomId, Map.of(
            "type", "CURSOR",
            "userId", ctx.userId,
            "username", ctx.username,
            "nickname", ctx.nickname,
            "x", x, "y", y,
            "selectionId", selectionId == null ? "" : selectionId
        ), ctx.userId);
    }

    private void handleEdit(WebSocketSession session, SessionContext ctx, JsonNode req) throws Exception {
        String op = req.path("op").asText("");
        JsonNode payload = req.path("payload");
        if (op.isEmpty()) {
            sendError(session, "edit op 不能为空");
            return;
        }
        // 持久化编辑操作 (用于回放)
        CollabMessage msg = collabService.saveMessage(ctx.roomId, ctx.userId, ctx.username, ctx.nickname,
                "EDIT", op, payload.toString(), req.path("clientMsgId").asText(null));
        // 广播给其他用户
        broadcastToRoom(ctx.roomId, Map.of(
            "type", "MESSAGE",
            "id", msg.getId(),
            "userId", ctx.userId,
            "messageType", "EDIT",
            "op", op,
            "payload", payload,
            "createdAt", msg.getCreatedAt().toString()
        ), ctx.userId);
    }

    private void handleAi(WebSocketSession session, SessionContext ctx, JsonNode req) throws Exception {
        String prompt = req.path("prompt").asText("");
        if (prompt.isEmpty()) {
            sendError(session, "AI prompt 不能为空");
            return;
        }

        // 简化版: 直接返回 mock 答案
        // 实际应注入 minimax-ai 的 MiniTransformer / AI Pipeline
        String aiAnswer = "[AI 协作] 收到: " + prompt + "\n\n这是模拟的协作 AI 响应. " +
                "生产环境会调用 minimax-ai 模块的 13 阶段 Pipeline 生成真实回复. " +
                "(V2.8.7 mock, 后续接入 minimax-ai)";

        // 持久化 AI 回复
        CollabMessage aiMsg = collabService.saveMessage(ctx.roomId, null, "ai", "AI Assistant",
                "AI", aiAnswer, null, null);

        // 流式推送给所有参与者
        String[] tokens = aiAnswer.split("(?<=\\s)|(?=\\s)");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            broadcastToRoom(ctx.roomId, Map.of(
                "type", "AI_CHUNK",
                "content", token,
                "finished", (i == tokens.length - 1),
                "msgId", aiMsg.getId()
            ), -1L);
            Thread.sleep(15); // 模拟流式延迟
        }
    }

    private void handleHeartbeat(WebSocketSession session, SessionContext ctx, JsonNode req) {
        collabService.heartbeat(ctx.roomId, ctx.userId, null, null, null);
        try {
            session.sendMessage(toJson(Map.of("type", "HEARTBEAT_ACK", "ts", System.currentTimeMillis())));
        } catch (Exception ignore) {}
    }

    // ============= 工具方法 =============

    private void broadcastToRoom(String roomId, Object message, Long excludeUserId) {
        List<WebSocketSession> sessions = collabService.roomSessions.get(roomId);
        if (sessions == null) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.warn("[collab] 序列化失败: {}", e.getMessage());
            return;
        }
        for (WebSocketSession s : sessions) {
            // 排除特定用户
            if (excludeUserId != null && excludeUserId > 0L) {
                SessionContext ctx = sessionContexts.get(s.getId());
                if (ctx != null && ctx.userId.equals(excludeUserId)) continue;
            }
            if (!s.isOpen()) continue;
            try {
                synchronized (s) {
                    s.sendMessage(new TextMessage(json));
                }
            } catch (Exception e) {
                log.warn("[collab] 推送失败 sessionId={}: {}", s.getId(), e.getMessage());
            }
        }
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            session.sendMessage(toJson(Map.of("type", "ERROR", "message", message)));
        } catch (Exception ignore) {}
    }

    private TextMessage toJson(Object obj) throws Exception {
        return new TextMessage(objectMapper.writeValueAsString(obj));
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new HashMap<>();
        if (uri == null || uri.getQuery() == null) return result;
        for (String pair : uri.getQuery().split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                try {
                    result.put(pair.substring(0, idx), java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    result.put(pair.substring(0, idx), pair.substring(idx + 1));
                }
            }
        }
        return result;
    }
}
