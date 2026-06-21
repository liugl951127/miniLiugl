package com.minimax.ws.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * V5.19: 双向 WebSocket 流式聊天.
 *
 * 服务端 → 客户端 (推):
 *   { type: "ready", streamId, model }
 *   { type: "chunk", content, streamId, progress }
 *   { type: "thinking", content, streamId }            // Agent 思考
 *   { type: "tool_call", name, args, streamId }
 *   { type: "observation", content, streamId }
 *   { type: "status", state, message, streamId }      // paused / resumed / cancelled
 *   { type: "done", streamId, finishReason }
 *   { type: "error", message, streamId }
 *
 * 客户端 → 服务端 (推):
 *   { action: "ping" }
 *   { action: "cancel" }
 *   { action: "pause" }                              // 暂停流, 服务端停止推 chunk
 *   { action: "resume" }                             // 恢复流
 *   { action: "steer", direction: "..." }            // 引导方向 ("更简洁" / "用代码")
 *   { action: "feedback", text, score }              // 评分反馈 (影响后续多轮)
 *   { action: "inject", messages: [{role,content}] } // 注入上下文 (RAG 等)
 *   { action: "set_model", model: "gpt-4o-mini" }    // 切换模型
 *
 * 路径: ws://host:8095/ws/bidi?type=chat&token=xxx&model=gpt-4o-mini
 *
 * @since V5.19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidirectionalStreamHandler extends TextWebSocketHandler {

    private final ObjectMapper json = new ObjectMapper();
    /** sessionId → WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    /** streamId → 状态对象 (含 paused / cancelled / steered messages) */
    private final Map<String, StreamState> states = new ConcurrentHashMap<>();

    /** 推流线程池 */
    private final ExecutorService pushPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ws-bidi-push");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        Map<String, String> params = parseQuery(session.getUri());
        String type = params.getOrDefault("type", "chat");
        String model = params.getOrDefault("model", "mock");
        String streamId = "b_" + UUID.randomUUID().toString().substring(0, 8);

        StreamState state = new StreamState();
        state.type = type;
        state.model = model;
        states.put(streamId, state);

        session.getAttributes().put("streamId", streamId);
        log.info("WS 双向连接建立: streamId={} type={} model={}", streamId, type, model);

        // 1) 推送 ready
        sendReady(session, streamId, model);

        // 2) 启动推流线程 (持续推 chunk)
        pushPool.submit(() -> runStream(session, streamId, state, params));
    }

    /**
     * V5.19 核心: 推流循环.
     * 检查 state.paused / state.cancelled / state.injectedMessages.
     */
    private void runStream(WebSocketSession session, String streamId, StreamState state, Map<String, String> params) {
        try {
            String type = state.type;
            switch (type) {
                case "chat":
                    streamChat(session, streamId, state);
                    break;
                case "agent":
                    streamAgent(session, streamId, state);
                    break;
                default:
                    sendError(session, streamId, "未知 type: " + type);
            }
        } catch (Exception e) {
            log.warn("WS 双向流异常: {}", e.getMessage());
            sendError(session, streamId, e.getMessage());
        } finally {
            states.remove(streamId);
        }
    }

    /**
     * Mock 流式聊天 (真实场景代理 chat 8082).
     * 演示: 暂停/恢复/引导/反馈 4 个交互能力.
     */
    private void streamChat(WebSocketSession session, String streamId, StreamState state) {
        String model = state.model;
        String prompt = state.steeredHint != null ? state.steeredHint : "你好";
        String feedback = state.lastFeedback != null ? " [反馈:" + state.lastFeedback + "]" : "";
        String injected = state.injectedMessages != null && !state.injectedMessages.isEmpty()
                ? " [注入:" + state.injectedMessages.size() + "条]" : "";

        String baseText = "你好, 我是 MiniMax V5.19 双向流演示. " +
                "你可以实时给我反馈 (评分/方向), 我会调整后续输出. " +
                "prompt = " + prompt + feedback + injected +
                ", model = " + model + ".";

        String[] words = baseText.split("");
        int idx = 0;
        while (idx < words.length) {
            // 检查状态
            if (state.cancelled) {
                sendDone(session, streamId, "cancelled");
                return;
            }
            if (state.paused) {
                sendStatus(session, streamId, "paused", "流已暂停, 等 resume");
                // 等待 resume (轮询 100ms)
                while (state.paused && !state.cancelled) {
                    Thread.sleep(100);
                }
                sendStatus(session, streamId, "resumed", "流已恢复");
            }
            // 推 5 个字
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5 && idx < words.length; i++) {
                sb.append(words[idx++]);
            }
            sendChunk(session, streamId, sb.toString(), (double) idx / words.length);
            Thread.sleep(50);
        }
        sendDone(session, streamId, "ok");
    }

    /**
     * Agent 流式 (复用 V5.16 SSE 事件, 翻译成 WS 双向事件).
     */
    private void streamAgent(WebSocketSession session, String streamId, StreamState state) {
        // 简化: 模拟 Agent 多步骤
        String[] steps = {
            "Thought: 用户想知道时间, 我需要调 get_current_time",
            "Action: get_current_time(timezone=Asia/Shanghai)",
            "Observation: 当前时间 2026-06-22 00:30",
            "Final: 现在是 2026-06-22 凌晨 00:30 (上海时区)"
        };
        int idx = 0;
        while (idx < steps.length) {
            if (state.cancelled) { sendDone(session, streamId, "cancelled"); return; }
            if (state.paused) {
                while (state.paused && !state.cancelled) Thread.sleep(100);
            }
            String step = steps[idx++];
            String type = step.startsWith("Thought:") ? "thinking"
                       : step.startsWith("Action:") ? "tool_call"
                       : step.startsWith("Observation:") ? "observation"
                       : "final";
            sendEvent(session, streamId, type, step);
            Thread.sleep(800);
        }
        sendDone(session, streamId, "ok");
    }

    /**
     * V5.19 核心: 接收客户端消息 (推→服务端).
     * 这是双向流的另一半, 之前 StreamGatewayHandler 没用到.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String streamId = (String) session.getAttributes().get("streamId");
        if (streamId == null) return;
        StreamState state = states.get(streamId);
        if (state == null) {
            sendError(session, "unknown", "streamId 不存在或已结束");
            return;
        }
        try {
            JsonNode msg = json.readTree(message.getPayload());
            String action = msg.path("action").asText("");
            if (action.isEmpty()) return;
            switch (action) {
                case "ping":
                    sendEvent(session, streamId, "pong", "{\"ts\":" + System.currentTimeMillis() + "}");
                    break;
                case "cancel":
                    state.cancelled = true;
                    sendStatus(session, streamId, "cancelled", "客户端已取消");
                    break;
                case "pause":
                    state.paused = true;
                    sendStatus(session, streamId, "paused", "客户端已暂停");
                    break;
                case "resume":
                    state.paused = false;
                    sendStatus(session, streamId, "resumed", "客户端已恢复");
                    break;
                case "steer":
                    state.steeredHint = msg.path("direction").asText("(无方向)");
                    sendStatus(session, streamId, "steered", "已引导: " + state.steeredHint);
                    break;
                case "feedback":
                    state.lastFeedback = msg.path("text").asText("");
                    int score = msg.path("score").asInt(0);
                    sendStatus(session, streamId, "feedback",
                        "已收到反馈: " + state.lastFeedback + " (score=" + score + ")");
                    break;
                case "inject":
                    JsonNode msgs = msg.path("messages");
                    if (msgs.isArray()) {
                        state.injectedMessages.clear();
                        msgs.forEach(m -> state.injectedMessages.add(m.asText()));
                        sendStatus(session, streamId, "injected",
                            "已注入 " + state.injectedMessages.size() + " 条上下文");
                    }
                    break;
                case "set_model":
                    String newModel = msg.path("model").asText("");
                    if (!newModel.isEmpty()) {
                        state.model = newModel;
                        sendStatus(session, streamId, "model_changed",
                            "模型已切换: " + newModel);
                    }
                    break;
                default:
                    sendStatus(session, streamId, "unknown_action", "未知 action: " + action);
            }
        } catch (Exception e) {
            sendError(session, streamId, "消息解析失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String streamId = (String) session.getAttributes().get("streamId");
        sessions.remove(session.getId());
        if (streamId != null) {
            StreamState state = states.get(streamId);
            if (state != null) state.cancelled = true;  // 通知推流线程退出
            states.remove(streamId);
        }
        log.info("WS 双向连接关闭: streamId={} status={}", streamId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WS 双向传输错误: {}", exception.getMessage());
        super.handleTransportError(session, exception);
    }

    // ---- helpers ----

    private void send(WebSocketSession session, ObjectNode node) {
        try {
            if (session.isOpen()) session.sendMessage(new TextMessage(node.toString()));
        } catch (Exception e) {
            log.debug("WS 发送失败: {}", e.getMessage());
        }
    }

    private void sendReady(WebSocketSession s, String streamId, String model) {
        ObjectNode n = json.createObjectNode();
        n.put("type", "ready"); n.put("streamId", streamId); n.put("model", model);
        n.put("bidirectional", true); n.put("version", "V5.19");
        send(s, n);
    }

    private void sendChunk(WebSocketSession s, String id, String content, double progress) {
        ObjectNode n = json.createObjectNode();
        n.put("type", "chunk"); n.put("content", content);
        n.put("streamId", id); n.put("progress", progress);
        send(s, n);
    }

    private void sendStatus(WebSocketSession s, String id, String state, String message) {
        ObjectNode n = json.createObjectNode();
        n.put("type", "status"); n.put("state", state);
        n.put("message", message); n.put("streamId", id);
        send(s, n);
    }

    private void sendEvent(WebSocketSession s, String id, String type, String content) {
        ObjectNode n = json.createObjectNode();
        n.put("type", type); n.put("content", content); n.put("streamId", id);
        send(s, n);
    }

    private void sendDone(WebSocketSession s, String id, String reason) {
        ObjectNode n = json.createObjectNode();
        n.put("type", "done"); n.put("streamId", id); n.put("finishReason", reason);
        send(s, n);
    }

    private void sendError(WebSocketSession s, String id, String msg) {
        ObjectNode n = json.createObjectNode();
        n.put("type", "error"); n.put("message", msg); n.put("streamId", id);
        send(s, n);
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> r = new java.util.HashMap<>();
        if (uri == null || uri.getQuery() == null) return r;
        for (String kv : uri.getQuery().split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) r.put(kv.substring(0, i), java.net.URLDecoder.decode(kv.substring(i + 1)));
        }
        return r;
    }

    /** V5.19 流状态 (跨线程共享, volatile/atomic) */
    public static class StreamState {
        public volatile boolean cancelled = false;
        public volatile boolean paused = false;
        public volatile String steeredHint = null;
        public volatile String lastFeedback = null;
        public volatile String model = "mock";
        public volatile String type = "chat";
        public final java.util.List<String> injectedMessages = new java.util.concurrent.CopyOnWriteArrayList<>();
    }
}
