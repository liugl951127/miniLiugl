package com.minimax.rag.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Random;

/**
 * RAG 用的 Mock Embedding (与 memory 同样的算法，保持一致)。
 * 离线可用，确定性。
 */
@Slf4j
@Primary
@Component
@ConditionalOnMissingBean(name = "ragOpenAiEmbeddingClient")
public class MockEmbeddingClient implements EmbeddingClient {

    @Value("${minimax.rag.embedding.dim:64}")
    private int dim;

    @Override
    public String code() { return "mock"; }
    @Override
    public int dim() { return dim; }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return new float[dim];
        String[] tokens = text.toLowerCase().replaceAll("\\s+", " ").split("[\\s,.，。、!?\\-]+");
        long[] features = new long[Math.max(1, tokens.length - 1)];
        for (int i = 0; i < features.length; i++) {
            features[i] = stableHash(tokens[i] + " " + (i+1 < tokens.length ? tokens[i+1] : ""));
        }
        float[] vec = new float[dim];
        Random r = new Random(0xC0FFEE);
        for (int i = 0; i < dim; i++) vec[i] = (float) r.nextGaussian();
        for (long f : features) {
            int idx = (int) (Math.abs(f) % dim);
            vec[idx] += 1.0f;
        }
        float norm = 0;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < dim; i++) vec[i] /= norm;
        return vec;
    }

    private long stableHash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) h = (h << 8) | (d[i] & 0xFF);
            return h;
        } catch (Exception e) { return s.hashCode(); }
    }
}
