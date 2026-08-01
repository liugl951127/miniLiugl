package com.bank.dualrecord.quality.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM Provider 接口(多模型适配)
 */
public interface LlmProvider {

    /**
     * 同步调用
     */
    String complete(String systemPrompt, String userPrompt);

    /**
     * 流式调用
     */
    void streamComplete(String systemPrompt, String userPrompt, StreamCallback callback);

    /**
     * 工具调用(Function Calling)
     */
    List<Map<String, Object>> functionCall(String systemPrompt, String userPrompt, List<Map<String, Object>> tools);

    /**
     * 健康检查
     */
    boolean healthCheck();

    /**
     * Provider ID
     */
    String getId();

    /**
     * 流式回调
     */
    interface StreamCallback {
        void onChunk(String chunk);
        void onComplete(String fullText);
        void onError(Throwable t);
    }
}
