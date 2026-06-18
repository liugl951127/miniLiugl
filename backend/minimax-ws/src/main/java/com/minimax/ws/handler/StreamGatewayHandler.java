package com.minimax.ws.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/**
 * WebSocket 流式网关 (V4.2).
 *
 * 连接: ws://host:8095/ws/stream?type=chat&token=xxx&sessionId=N&model=gpt-4o-mini&prompt=hi
 *
 * 支持 type:
 *   - chat:    流式聊天 (代理 chat 8082)
 *   - vision:  图片 + 文本 → vision 模型流式输出
 *   - audio:   TTS 流式合成
 *   - agent:   Agent 实时进度推送
 *   - battle:  多模型对决实时
 *
 * 协议:
 *   客户端 → 服务端:
 *     {"action": "subscribe", "topic": "..."}
 *     {"action": "cancel", "streamId": "..."}
 *     {"action": "ping"}
 *
 *   服务端 → 客户端:
 *     {"type": "ready", "streamId": "..."}
 *     {"type": "chunk", "content": "...", "streamId": "..."}
 *     {"type": "tool_call", "name": "...", "args": {...}, "streamId": "..."}
 *     {"type": "done", "streamId": "..."}
 *     {"type": "error", "message": "...", "streamId": "..."}
 *
 * @since 2026-06
 */
@Slf4j
@Component
public class StreamGatewayHandler extends TextWebSocketHandler {

    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cancelFlags = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        Map<String, String> params = parseQuery(session.getUri());
        String type = params.getOrDefault("type", "chat");
        String streamId = "s_" + UUID.randomUUID().toString().substring(0, 8);
        session.getAttributes().put("streamId", streamId);
        session.getAttributes().put("type", type);
        log.info("WS 连接建立: id={} type={} remote={}", streamId, type, session.getRemoteAddress());

        // 推送 ready
        ObjectNode ready = json.createObjectNode();
        ready.put("type", "ready");
        ready.put("streamId", streamId);
        ready.put("streamType", type);
        send(session, ready);

        // 异步启动流
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.submit(() -> handleStream(session, type, params));
    }

    private void handleStream(WebSocketSession session, String type, Map<String, String> params) {
        String streamId = (String) session.getAttributes().get("streamId");
        try {
            switch (type) {
                case "chat":
                    streamChat(session, params, streamId);
                    break;
                case "vision":
                    streamVision(session, params, streamId);
                    break;
                case "audio":
                    streamTts(session, params, streamId);
                    break;
                case "battle":
                    streamBattle(session, params, streamId);
                    break;
                case "agent":
                    streamAgent(session, params, streamId);
                    break;
                default:
                    sendError(session, "未知 type: " + type, streamId);
            }
        } catch (Exception e) {
            log.warn("WS 流异常: {}", e.getMessage());
            sendError(session, e.getMessage(), streamId);
        }
    }

    private void streamChat(WebSocketSession session, Map<String, String> params, String streamId) {
        String prompt = params.getOrDefault("prompt", "你好");
        String model = params.getOrDefault("model", "mock");
        cancelFlags.put(streamId, false);

        // 模拟流式输出 (mock)
        String[] words = ("你好, 我是 MiniMax 智能助手. " +
                "这是 WebSocket 流式输出演示. " +
                "你可以接入真实 chat 8082 SSE, " +
                "通过这个连接实时推送每个 token. " +
                "prompt = " + prompt + ", model = " + model).split("");
        int idx = 0;
        while (idx < words.length) {
            if (cancelFlags.getOrDefault(streamId, false)) {
                sendDone(session, streamId, "cancelled");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5 && idx < words.length; i++) {
                sb.append(words[idx++]);
            }
            ObjectNode chunk = json.createObjectNode();
            chunk.put("type", "chunk");
            chunk.put("content", sb.toString());
            chunk.put("streamId", streamId);
            chunk.put("progress", (double) idx / words.length);
            send(session, chunk);
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        sendDone(session, streamId, "ok");
    }

    private void streamVision(WebSocketSession session, Map<String, String> params, String streamId) {
        cancelFlags.put(streamId, false);
        String[] lines = {
            "我看到一张图片. ",
            "整体色调偏暖. ",
            "主体似乎是一个橘色的小动物. ",
            "细节: 大眼睛, 毛茸茸. ",
            "背景虚化, 浅景深. ",
            "判断: 应该是橘猫. ",
            "情绪: 放松, 满足."
        };
        for (String line : lines) {
            if (cancelFlags.getOrDefault(streamId, false)) { sendDone(session, streamId, "cancelled"); return; }
            ObjectNode chunk = json.createObjectNode();
            chunk.put("type", "chunk");
            chunk.put("content", line);
            chunk.put("streamId", streamId);
            send(session, chunk);
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        sendDone(session, streamId, "ok");
    }

    private void streamTts(WebSocketSession session, Map<String, String> params, String streamId) {
        // 实际生产: 流式返回 mp3 chunk (base64)
        String text = params.getOrDefault("text", "你好");
        String[] chunks = splitText(text, 5);
        for (String chunk : chunks) {
            ObjectNode node = json.createObjectNode();
            node.put("type", "audio_chunk");
            node.put("text", chunk);
            node.put("streamId", streamId);
            send(session, node);
            try { Thread.sleep(80); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        sendDone(session, streamId, "ok");
    }

    private void streamBattle(WebSocketSession session, Map<String, String> params, String streamId) {
        String[] models = (params.get("models") != null ? params.get("models") : "mock,gpt-4o-mini,qwen-max").split(",");
        for (String m : models) {
            if (cancelFlags.getOrDefault(streamId, false)) { sendDone(session, streamId, "cancelled"); return; }
            ObjectNode start = json.createObjectNode();
            start.put("type", "model_start");
            start.put("model", m);
            start.put("streamId", streamId);
            send(session, start);

            // 模拟流
            String txt = "[ " + m + " ] " + "思考中...";
            for (char c : txt.toCharArray()) {
                ObjectNode chunk = json.createObjectNode();
                chunk.put("type", "chunk");
                chunk.put("model", m);
                chunk.put("content", String.valueOf(c));
                chunk.put("streamId", streamId);
                send(session, chunk);
                try { Thread.sleep(15); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            ObjectNode done = json.createObjectNode();
            done.put("type", "model_done");
            done.put("model", m);
            done.put("latencyMs", (long)(Math.random() * 2000));
            done.put("streamId", streamId);
            send(session, done);
        }
        sendDone(session, streamId, "ok");
    }

    private void streamAgent(WebSocketSession session, Map<String, String> params, String streamId) {
        String goal = params.getOrDefault("goal", "搜索竞品价格");
        String[] rounds = {
            "Round 1: 思考中... 我需要搜索 3 个竞品",
            "Round 1: 调工具 web_search('竞品价格')",
            "Round 1: 观察到 5 个结果",
            "Round 2: 思考中... 提取价格信息",
            "Round 2: 调工具 parse_html(结果)",
            "Round 2: 观察到 3 个 SKU",
            "Round 3: 思考中... 整理成表格",
            "Round 3: ✓ 完成\n\n竞品 A: ¥99\n竞品 B: ¥129\n竞品 C: ¥89"
        };
        for (String r : rounds) {
            if (cancelFlags.getOrDefault(streamId, false)) { sendDone(session, streamId, "cancelled"); return; }
            ObjectNode chunk = json.createObjectNode();
            chunk.put("type", "agent_step");
            chunk.put("step", r);
            chunk.put("streamId", streamId);
            send(session, chunk);
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        sendDone(session, streamId, "ok");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ObjectNode req = (ObjectNode) json.readTree(message.getPayload());
            String action = req.path("action").asText("");
            String streamId = (String) session.getAttributes().get("streamId");

            switch (action) {
                case "cancel":
                    cancelFlags.put(streamId, true);
                    ObjectNode ack = json.createObjectNode();
                    ack.put("type", "cancel_ack");
                    ack.put("streamId", streamId);
                    send(session, ack);
                    break;
                case "ping":
                    ObjectNode pong = json.createObjectNode();
                    pong.put("type", "pong");
                    pong.put("ts", System.currentTimeMillis());
                    send(session, pong);
                    break;
                default:
                    log.debug("未知 action: {}", action);
            }
        } catch (Exception e) {
            log.warn("WS 消息解析失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String streamId = (String) session.getAttributes().get("streamId");
        cancelFlags.put(streamId, true);
        sessions.remove(session.getId());
        log.info("WS 关闭: id={} status={}", streamId, status);
    }

    private void send(WebSocketSession session, ObjectNode payload) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload.toString()));
            }
        } catch (Exception e) {
            log.warn("WS 发送失败: {}", e.getMessage());
        }
    }

    private void sendDone(WebSocketSession session, String streamId, String reason) {
        ObjectNode done = json.createObjectNode();
        done.put("type", "done");
        done.put("streamId", streamId);
        done.put("reason", reason);
        send(session, done);
        cancelFlags.remove(streamId);
    }

    private void sendError(WebSocketSession session, String msg, String streamId) {
        ObjectNode err = json.createObjectNode();
        err.put("type", "error");
        err.put("message", msg);
        err.put("streamId", streamId);
        send(session, err);
        cancelFlags.remove(streamId);
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        if (uri == null || uri.getQuery() == null) return params;
        for (String pair : uri.getQuery().split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                params.put(pair.substring(0, idx), java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private String[] splitText(String s, int n) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < s.length(); i += n) {
            list.add(s.substring(i, Math.min(i + n, s.length())));
        }
        return list.toArray(new String[0]);
    }
}
