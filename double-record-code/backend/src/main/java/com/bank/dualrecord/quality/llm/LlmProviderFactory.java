package com.bank.dualrecord.quality.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM Provider 工厂 + 主备切换
 */
@Slf4j
@Component
public class LlmProviderFactory {

    private final List<LlmProvider> providers;
    private final Map<String, LlmProvider> providerMap;

    @Value("${llm.primary:qwen}")
    private String primary;

    @Value("${llm.fallback:deepseek}")
    private String fallback;

    public LlmProviderFactory(List<LlmProvider> providers) {
        this.providers = providers;
        this.providerMap = new java.util.HashMap<>();
        for (LlmProvider p : providers) {
            providerMap.put(p.getId(), p);
        }
    }

    /**
     * 获取主 Provider,失败时自动 fallback
     */
    public LlmProvider getProvider() {
        LlmProvider p = providerMap.get(primary);
        if (p != null && p.healthCheck()) {
            return p;
        }
        log.warn("主 LLM Provider {} 不可用,降级到 {}", primary, fallback);
        LlmProvider fb = providerMap.get(fallback);
        if (fb != null && fb.healthCheck()) {
            return fb;
        }
        throw new RuntimeException("所有 LLM Provider 都不可用");
    }

    public LlmProvider getById(String id) {
        return providerMap.get(id);
    }

    public List<String> getAvailableIds() {
        return providers.stream().filter(LlmProvider::healthCheck).map(LlmProvider::getId).toList();
    }
}
