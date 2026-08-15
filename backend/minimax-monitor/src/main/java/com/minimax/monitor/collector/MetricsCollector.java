package com.minimax.monitor.collector;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标采点。
 *
 * 暴露 5 类指标 (所有服务通用):
 *  - Counter:  chat_messages_total / tool_calls_total / rag_queries_total / llm_tokens_total / errors_total
 *  - Gauge:    active_sessions / kb_count / user_count
 *  - Timer:    request_duration / llm_latency / tool_duration
 *
 * 用法:
 *   metricsCollector.incChatMessages();
 *   metricsCollector.timeLlmLatency(() -> callLlm());
 */
@Component
@RequiredArgsConstructor
public class MetricsCollector {

    private final MeterRegistry registry;

    // ---- Counters ----
    private Counter chatMessages;
    private Counter toolCalls;
    private Counter ragQueries;
    private Counter llmTokens;
    private Counter http5xx;
    private Counter http4xx;
    private Counter uploads;

    // ---- Gauges (用 AtomicLong 支持外部修改) ----
    private final AtomicLong activeSessions = new AtomicLong(0);
    private final AtomicLong kbCount = new AtomicLong(0);
    private final AtomicLong userCount = new AtomicLong(0);
    private final AtomicLong memoryCount = new AtomicLong(0);

    public void init() {
        chatMessages = Counter.builder("minimax_chat_messages_total")
                .description("总聊天消息数")
                .register(registry);
        toolCalls = Counter.builder("minimax_tool_calls_total")
                .description("总工具调用数")
                .register(registry);
        ragQueries = Counter.builder("minimax_rag_queries_total")
                .description("总 RAG 查询数")
                .register(registry);
        llmTokens = Counter.builder("minimax_llm_tokens_total")
                .description("总 LLM token 数")
                .register(registry);
        http5xx = Counter.builder("minimax_http_5xx_total")
                .description("HTTP 5xx 错误数")
                .register(registry);
        http4xx = Counter.builder("minimax_http_4xx_total")
                .description("HTTP 4xx 错误数")
                .register(registry);
        uploads = Counter.builder("minimax_uploads_total")
                .description("文件上传数")
                .register(registry);

        // 注册 Gauges
        registry.gauge("minimax_active_sessions", activeSessions);
        registry.gauge("minimax_kb_count", kbCount);
        registry.gauge("minimax_user_count", userCount);
        registry.gauge("minimax_memory_count", memoryCount);
    }

    // ---- API ----

    public void incChatMessages() { chatMessages.increment(); }
    public void incChatMessages(long n) { chatMessages.increment(n); }
    public void incToolCalls() { toolCalls.increment(); }
    public void incRagQueries() { ragQueries.increment(); }
    public void incLlmTokens(long n) { llmTokens.increment(n); }
    public void incHttp5xx() { http5xx.increment(); }
    public void incHttp4xx() { http4xx.increment(); }
    public void incUploads() { uploads.increment(); }

    public void setActiveSessions(long n) { activeSessions.set(n); }
    public void setKbCount(long n) { kbCount.set(n); }
    public void setUserCount(long n) { userCount.set(n); }
    public void setMemoryCount(long n) { memoryCount.set(n); }

    public long getActiveSessions() { return activeSessions.get(); }
    public long getKbCount() { return kbCount.get(); }
    public long getUserCount() { return userCount.get(); }
    public long getMemoryCount() { return memoryCount.get(); }

    public double getChatMessages() { return chatMessages.count(); }
    public double getToolCalls() { return toolCalls.count(); }
    public double getRagQueries() { return ragQueries.count(); }
    public double getLlmTokens() { return llmTokens.count(); }
    public double getHttp5xx() { return http5xx.count(); }
    public double getHttp4xx() { return http4xx.count(); }
    public double getUploads() { return uploads.count(); }

    /** 计时 LLM 调用 */
    public Timer.Sample startTimer() { return Timer.start(registry); }
    public void recordLlmLatency(Timer.Sample sample) {
        sample.stop(Timer.builder("minimax_llm_latency")
                .description("LLM 调用延迟")
                .publishPercentileHistogram()
                .register(registry));
    }

    public void recordToolDuration(String toolName, Duration d) {
        Timer.builder("minimax_tool_duration")
                .description("工具调用延迟")
                .tag("tool", toolName)
                .publishPercentileHistogram()
                .register(registry)
                .record(d);
    }
}
