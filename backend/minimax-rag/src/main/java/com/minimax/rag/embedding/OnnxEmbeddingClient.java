package com.minimax.rag.embedding;

import com.minimax.rag.onnx.OnnxInferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * ONNX Embedding Client (V7.0)
 * 优先于 MockEmbeddingClient (通过 @ConditionalOnBean)
 *
 * 当 OnnxInferenceService 成功初始化时生效:
 *  - minimax.rag.onnx.enabled=true
 *  - minimax.rag.onnx.model-path 或 model-url 已配置
 */
@Slf4j
@Component
@ConditionalOnBean(OnnxInferenceService.class)
public class OnnxEmbeddingClient implements EmbeddingClient {

    private final OnnxInferenceService inference;

    public OnnxEmbeddingClient(@Autowired(required = false) OnnxInferenceService inference) {
        this.inference = inference;
    }

    @Override
    public String code() { return "onnx"; }

    @Override
    public float[] embed(String text) {
        if (inference == null || !inference.isInitialized()) {
            log.warn("[OnnxEmbedding] ONNX 未初始化，回退到空向量");
            return new float[inference != null ? inference.getEmbeddingDim() : 64];
        }
        float[] vec = inference.infer(text);
        if (vec == null) {
            log.warn("[OnnxEmbedding] 推理失败，返回零向量");
            return new float[inference.getEmbeddingDim()];
        }
        return vec;
    }

    @Override
    public int dim() {
        return inference != null ? inference.getEmbeddingDim() : 64;
    }
}
