package com.minimax.model.provider;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 模型 Provider 适配器接口。
 * 任意兼容 OpenAI / Anthropic / Ollama / Minimax-M3 协议的供应商都实现此接口。
 *
 * 两个调用形式：
 *  1. 阻塞 chat()：返回完整响应
 *  2. 流式 streamChat()：以回调方式推每个 chunk（SSE JSON 字符串）
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
     * 流式调用（回调式）。每个 chunk 是一段 SSE JSON 字符串（不含 "data: " 前缀）。
     * 取消时抛 RuntimeException("STREAM_CANCELLED") 由调用方捕获。
     *
     * @return OpenAiCompatibleAdapter.StreamResult 包含完整 token 统计
     */
    default OpenAiCompatibleAdapter.StreamResult streamChat(String endpoint, String apiKey, ChatRequest req,
                                                             Consumer<String> chunkJsonConsumer,
                                                             AtomicBoolean stopFlag) {
        throw new UnsupportedOperationException(code() + " 不支持流式");
    }

    /**
     * 兼容旧流式 API（webflux Flux）。Day 4 保留。
     */
    Flux<String> stream(String endpoint, String apiKey, ChatRequest req);

    /** 健康检查。 */
    default boolean ping(String endpoint, String apiKey) {
        return true;
    }
}
