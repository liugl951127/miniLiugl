package com.minimax.model.provider;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 模型 Provider 适配器接口。
 * 任意兼容 OpenAI / Anthropic / Ollama / Minimax-M3 协议的供应商都实现此接口。
 *
 * 两个调用形式：
 *  1. 阻塞 chat()：返回完整响应
 *  2. 流式 stream()：返回 SSE 字符串片段（data: {...}\n\n）
 */
public interface ModelProviderAdapter {

    /** provider code，匹配 model_provider.code。 */
    String code();

    /**
     * 阻塞调用。
     * @param endpoint  provider base_url
     * @param apiKey    provider api_key（Ollama 等本地模型可为 null）
     * @param req       ChatRequest
     */
    ChatResponse chat(String endpoint, String apiKey, ChatRequest req);

    /**
     * 流式调用。
     * 返回 SSE 片段（"data: {...}\n\n" 形式），最后一段为 "data: [DONE]\n\n"。
     */
    Flux<String> stream(String endpoint, String apiKey, ChatRequest req);

    /** 健康检查（可选，Ollama 用于探活）。 */
    default boolean ping(String endpoint, String apiKey) {
        return true;
    }
}
