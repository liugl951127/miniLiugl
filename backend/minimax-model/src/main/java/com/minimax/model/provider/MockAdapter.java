package com.minimax.model.provider;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 离线 Mock 适配器：mock 模式时所有调用都走这里。
 * - chat() 阻塞返回一个固定格式回复
 * - streamChat() 按 30ms 字符节奏流式输出
 *
 * Day 5: 升级为支持 stopFlag 取消
 */
@Slf4j
@Component
public class MockAdapter implements ModelProviderAdapter {

    @Override
    public String code() { return "mock"; }

    @Override
    public ChatResponse chat(String endpoint, String apiKey, ChatRequest req) {
        Instant t0 = Instant.now();
        String lastUser = lastUserContent(req);
        String content = String.format(
            "【Mock 回复】你调用的是 %s。\n你说: \"%s\"\nDay 4 路由层已就位，Day 5 接入真实流式。",
            req.getModel(), truncate(lastUser, 200));

        int pt = approxTokens(lastUser);
        int ct = approxTokens(content);

        Map<String, Object> raw = new HashMap<>();
        raw.put("id", "mock-" + System.currentTimeMillis());
        raw.put("model", req.getModel());
        raw.put("object", "chat.completion");
        raw.put("usage", Map.of("prompt_tokens", pt, "completion_tokens", ct, "total_tokens", pt+ct));

        return ChatResponse.builder()
                .id((String) raw.get("id"))
                .model(req.getModel())
                .content(content)
                .promptTokens(pt)
                .completionTokens(ct)
                .totalTokens(pt + ct)
                .finishReason("stop")
                .latencyMs(Duration.between(t0, Instant.now()).toMillis())
                .providerCode(code())
                .raw(raw)
                .build();
    }

    /**
     * 流式 mock：30ms 推一个字符，完整 SSE chunk。
     * consumer 拿到的是 JSON 字符串（不含 "data: " 前缀），controller 加上前缀写出去。
     */
    public OpenAiCompatibleAdapter.StreamResult streamChat(ChatRequest req,
                                                           Consumer<String> chunkJsonConsumer,
                                                           AtomicBoolean stopFlag) throws InterruptedException {
        Instant t0 = Instant.now();
        String lastUser = lastUserContent(req);
        String text = "【Mock 流式】你调用 " + req.getModel() + "，消息 \"" + truncate(lastUser, 80) + "\"。\n"
                + "Day 5 流式响应。\n"
                + "每 30ms 推一个字符。\n"
                + "你可以随时取消。";
        StringBuilder acc = new StringBuilder();
        String id = "mock-stream-" + System.currentTimeMillis();

        for (char c : text.toCharArray()) {
            if (stopFlag != null && stopFlag.get()) break;
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            acc.append(c);
            String json = "{\"id\":\"" + id + "\",\"object\":\"chat.completion.chunk\","
                    + "\"model\":\"" + req.getModel() + "\","
                    + "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + escapeJson(c+"") + "\"}}]}";
            chunkJsonConsumer.accept(json);
        }

        int pt = approxTokens(lastUser);
        int ct = approxTokens(text);
        boolean cancelled = stopFlag != null && stopFlag.get();
        return new OpenAiCompatibleAdapter.StreamResult(
                id, req.getModel(), acc.toString(),
                pt, ct, pt + ct,
                cancelled ? "cancelled" : "stop",
                Duration.between(t0, Instant.now()).toMillis()
        );
    }

    @Override
    public Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        return Flux.error(new UnsupportedOperationException("Mock stream 通过 streamChat() 调用"));
    }

    private String lastUserContent(ChatRequest req) {
        if (req.getMessages() == null || req.getMessages().isEmpty()) return "";
        for (int i = req.getMessages().size() - 1; i >= 0; i--) {
            Map<String, String> m = req.getMessages().get(i);
            if ("user".equals(m.get("role"))) {
                return m.getOrDefault("content", "");
            }
        }
        return req.getMessages().get(req.getMessages().size() - 1).getOrDefault("content", "");
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private int approxTokens(String s) {
        if (s == null) return 0;
        return Math.max(1, s.length() / 2);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
