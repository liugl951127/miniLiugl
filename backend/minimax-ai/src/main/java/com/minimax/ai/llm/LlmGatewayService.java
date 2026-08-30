package com.minimax.ai.llm;

import com.minimax.ai.llm.onnx.OnnxQwenChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LLM 统一网关 (V9.0) — 面客级 LLM 兜底层
 *
 * 设计:
 *  1. 所有微服务 (chat/analytics/agent/rule/rag) 调这个, 不直接调云端
 *  2. 一次调用, 自动 cloud → local 兜底, 业务层无感
 *  3. 响应带 source 标识, 前端可显式标注
 *
 * 配置 (application.yml):
 *   agent-forge:
 *     llm:
 *       primary-model: gpt-4o-mini     # 云端主用
 *       fallback-model: qwen2.5-0.5b   # 本地兜底 (在 minimax-ai 服务内)
 *       timeout-ms: 8000               # 云端超时切本地
 *       fallback-enabled: true         # 是否启用兜底
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LlmGatewayService {

    private final OnnxQwenChatService qwen;  // 注入本地 Qwen2.5-0.5B 服务
    private final CloudLlmClient cloudLlm;  // 注入云端 LLM 客户端

    @Value("${agent-forge.llm.primary-model:gpt-4o-mini}")
    private String primaryModel;

    @Value("${agent-forge.llm.fallback-model:qwen2.5-0.5b-instruct}")
    private String fallbackModel;

    @Value("${agent-forge.llm.timeout-ms:8000}")
    private int timeoutMs;

    @Value("${agent-forge.llm.fallback-enabled:true}")
    private boolean fallbackEnabled;

    public enum Source { CLOUD, LOCAL, LOCAL_FALLBACK, UNAVAILABLE }

    public record ChatResult(
        String content,
        Source source,
        String model,
        long durationMs,
        String reason  // 兜底原因 / 错误信息
    ) {}

    /**
     * 统一 chat 入口 — 自动兜底
     */
    public ChatResult chat(List<Map<String, String>> messages) {
        long start = System.currentTimeMillis();

        // 1. 尝试云端
        if (cloudLlm.isConfigured()) {
            try {
                String content = cloudLlm.chat(messages, primaryModel, timeoutMs);
                long dur = System.currentTimeMillis() - start;
                log.info("[LLM] ☁️  cloud OK, model={}, {}ms", primaryModel, dur);
                return new ChatResult(content, Source.CLOUD, primaryModel, dur, null);
            } catch (Exception e) {
                log.warn("[LLM] ☁️  cloud 失败: {} (model={})", e.getMessage(), primaryModel);
                if (!fallbackEnabled || !qwen.isReady()) {
                    long dur = System.currentTimeMillis() - start;
                    return new ChatResult(null, Source.UNAVAILABLE, primaryModel, dur,
                        "云端失败: " + e.getMessage() + (fallbackEnabled ? "本地未就绪" : "兜底关闭"));
                }
                // 进入本地兜底
            }
        } else {
            log.info("[LLM] ☁️  cloud 未配置, 直接走本地");
        }

        // 2. 本地兜底 (Qwen2.5-0.5B)
        if (qwen.isReady()) {
            try {
                String prompt = messagesToPrompt(messages);
                var local = qwen.chat(prompt, null, 1024);
                long dur = System.currentTimeMillis() - start;
                if (local.isSuccess()) {
                    String reason = cloudLlm.isConfigured() ? "云端失败" : "云端未配置";
                    log.info("[LLM] 💻 local OK, model={}, {}ms ({})", fallbackModel, dur, reason);
                    return new ChatResult(local.content(), Source.LOCAL_FALLBACK, fallbackModel, dur, reason);
                }
                log.warn("[LLM] 💻 local 失败: {}", local.error());
            } catch (Exception e) {
                log.warn("[LLM] 💻 local 异常: {}", e.getMessage());
            }
        } else {
            log.error("[LLM] 💻 local 未就绪, Qwen2.5-0.5B 模型未下载或加载失败");
        }

        // 3. 全失败
        long dur = System.currentTimeMillis() - start;
        return new ChatResult(null, Source.UNAVAILABLE, primaryModel, dur,
            "云端失败 + 本地不可用, 请检查 Qwen2.5-0.5B 模型状态 (执行 scripts/download-models.sh qwen)");
    }

    private String messagesToPrompt(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        for (var m : messages) {
            String role = m.getOrDefault("role", "user");
            String content = m.getOrDefault("content", "");
            if ("system".equals(role)) {
                sb.append("System: ").append(content).append("\n\n");
            } else if ("assistant".equals(role)) {
                sb.append("Assistant: ").append(content).append("\n\n");
            } else {
                sb.append("User: ").append(content).append("\n\n");
            }
        }
        sb.append("Assistant:");
        return sb.toString();
    }

    public boolean isLocalReady() { return qwen.isReady(); }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
}
