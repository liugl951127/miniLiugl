package com.minimax.common.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * LLM 客户端 SDK (V9.0/V9.0.1) — 共享给所有微服务
 *
 * V9.0.1 修致命错: 自己 new RestTemplate, 不依赖外部 Bean
 *   (minimax-common 没提供 RestTemplate bean, 之前用 @RequiredArgsConstructor 注入会 NoSuchBean)
 *
 * 各业务服务 (chat/analytics/agent/rule/rag) 调这个类即可获得
 *  cloud→local 兜底, 不用自己实现重试/降级.
 *
 * 内部 HTTP 调 minimax-ai 的 /api/v1/ai/llm/chat
 *  minimax-ai 服务里跑着 Qwen2.5-0.5B (本地兜底)
 *
 * 用法:
 *   @Autowired LlmClient llm;
 *   LlmResult r = llm.chat(List.of(Map.of("role", "user", "content", "...")));
 *   if (r.isAvailable()) { show(r.content); }
 *   else { show("AI 暂时不可用: " + r.reason); }
 */
@Component
@Slf4j
public class LlmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${minimax-ai.url:http://localhost:8090}")
    private String aiServiceUrl;

    @Value("${minimax-ai.llm-timeout-ms:30000}")
    private int timeoutMs;

    /** 自己 new, 不依赖 Spring 容器 (关键: 解决 V9.0 致命错) */
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
        log.info("[LlmClient] 初始化完成, minimax-ai URL: {}, timeout: {}ms", aiServiceUrl, timeoutMs);
    }

    public enum Source { CLOUD, LOCAL, LOCAL_FALLBACK, UNAVAILABLE }

    public record LlmResult(
        String content,
        Source source,
        String model,
        long durationMs,
        String reason,
        boolean available
    ) {
        public boolean isLocal() { return source == Source.LOCAL || source == Source.LOCAL_FALLBACK; }
        public boolean isCloud() { return source == Source.CLOUD; }
    }

    /**
     * 调 LLM (cloud→local 自动兜底, 由 minimax-ai 处理)
     */
    public LlmResult chat(List<Map<String, String>> messages) {
        long start = System.currentTimeMillis();
        try {
            String url = aiServiceUrl + "/api/v1/ai/llm/chat";
            Map<String, Object> body = Map.of("messages", messages);
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            // 应用层超时
            HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(body), h);
            // 简单实现: 用 connect/read timeout (前提: RestTemplate 已配)
            var resp = restTemplate.postForEntity(url, entity, Map.class);
            long dur = System.currentTimeMillis() - start;
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return new LlmResult(null, Source.UNAVAILABLE, "unknown", dur,
                    "minimax-ai 返回 " + resp.getStatusCode(), false);
            }
            Map<?, ?> data = resp.getBody();
            String content = (String) data.get("content");
            String sourceStr = (String) data.get("source");
            String model = (String) data.get("model");
            Number durationMs = (Number) data.get("durationMs");
            String reason = (String) data.get("reason");
            Source source = parseSource(sourceStr);
            boolean available = content != null && !content.isBlank();
            return new LlmResult(content, source, model,
                durationMs != null ? durationMs.longValue() : dur, reason, available);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            long dur = System.currentTimeMillis() - start;
            log.error("[LlmClient] minimax-ai 网络失败: {}", e.getMessage());
            return new LlmResult(null, Source.UNAVAILABLE, "unknown", dur,
                "minimax-ai 不可达: " + e.getMessage(), false);
        } catch (Exception e) {
            long dur = System.currentTimeMillis() - start;
            log.error("[LlmClient] 异常: {}", e.getMessage());
            return new LlmResult(null, Source.UNAVAILABLE, "unknown", dur,
                e.getMessage(), false);
        }
    }

    /**
     * 便捷方法: 单轮 user 消息
     */
    public LlmResult chat(String userMessage) {
        return chat(List.of(Map.of("role", "user", "content", userMessage)));
    }

    /**
     * 便捷方法: system + user
     */
    public LlmResult chat(String system, String userMessage) {
        return chat(List.of(
            Map.of("role", "system", "content", system),
            Map.of("role", "user", "content", userMessage)
        ));
    }

    /**
     * 健康检查: minimax-ai 是否就绪
     */
    public boolean isAiServiceReady() {
        try {
            String url = aiServiceUrl + "/api/v1/ai/llm/status";
            var resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object ready = resp.getBody().get("localReady");
                return Boolean.TRUE.equals(ready);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private Source parseSource(String s) {
        if (s == null) return Source.UNAVAILABLE;
        try { return Source.valueOf(s); } catch (Exception e) { return Source.UNAVAILABLE; }
    }
}
