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

/**
 * 离线 Mock 适配器：当没有可用 API key 时启用，给前端"能演示"用。
 * - chat()  阻塞返回一个固定格式回复
 * - stream() 流式按字吐出回复
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
            "【Mock 回复】你调用的是 %s。\n" +
            "你说: \"%s\"\n" +
            "Day 4 路由层已就位，Day 5 接入真实流式。",
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

    @Override
    public Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        String lastUser = lastUserContent(req);
        String text = "【Mock 流式】你调用 " + req.getModel() + "，消息 \"" + truncate(lastUser, 80) + "\"。";
        return Flux.fromArray(text.split(""))
                .delayElements(Duration.ofMillis(30))
                .map(c -> {
                    Map<String, Object> chunk = new HashMap<>();
                    chunk.put("id", "mock-stream");
                    chunk.put("object", "chat.completion.chunk");
                    chunk.put("model", req.getModel());
                    chunk.put("choices", List.of(Map.of(
                            "index", 0,
                            "delta", Map.of("content", c),
                            "finish_reason", null
                    )));
                    try {
                        return "data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(chunk) + "\n\n";
                    } catch (Exception e) {
                        return "data: {}\n\n";
                    }
                })
                .concatWith(Flux.just("data: [DONE]\n\n"));
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
        // 粗估: 1 token ≈ 1.5 字符（中文为主时更接近 1）
        return Math.max(1, s.length() / 2);
    }
}
