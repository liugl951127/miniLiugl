package com.minimax.model.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 根据 provider code 选择对应的 adapter。
 * 如果有多个候选（OpenAI/Minimax-M3/Ollama 都用 openai 协议）→ 都用 OpenAiCompatibleAdapter。
 * 找不到时回退到 Mock。
 *
 * minmax.model.mock-mode=true 时 全部强制走 mock（演示/测试用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelProviderFactory {

    @Value("${minimax.model.mock-mode:false}")
    private boolean mockMode;

    private final List<ModelProviderAdapter> adapters;

    private Map<String, ModelProviderAdapter> cache;

    private Map<String, ModelProviderAdapter> map() {
        if (cache == null) {
            cache = adapters.stream().collect(Collectors.toMap(ModelProviderAdapter::code, a -> a, (a, b) -> a));
        }
        return cache;
    }

    public ModelProviderAdapter get(String code) {
        if (code == null) {
            log.warn("model code 为 null, 回退到 mock");
            return map().get("mock");
        }
        if (mockMode) {
            log.debug("mock-mode 开启，强制返回 MockAdapter");
            return map().get("mock");
        }
        ModelProviderAdapter a = map().get(code);
        if (a != null) return a;
        // 自研 ONNX 模型 → OnnxLLMAdapter
        if ("self-trained".equals(code) || "onnx".equals(code)) {
            return map().get("onnx");
        }
        // 兼容 OpenAI 协议的多个供应商：openai / minimax / ollama / zhipu / qwen / deepseek / local
        if (List.of("openai","minimax","ollama","zhipu","qwen","deepseek","local").contains(code)) {
            return map().get("openai");
        }
        log.warn("未找到 provider={} 的 adapter, 回退到 mock", code);
        return map().get("mock");
    }
}
