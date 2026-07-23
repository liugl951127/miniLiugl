package com.minimax.chat.memory_ext.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Random;

/**
 * 离线 Mock Embedding：基于文本内容的稳定哈希生成"伪向量"。
 * 语义特征：相同/相似文本 → 余弦相似度接近 1.0。
 *
 * 用途：
 *  1. 没有真实 embedding API 时也能演示向量召回
 *  2. 单元测试
 *  3. 开发环境离线开发
 *
 * ⚠️ 不是真实语义向量，但能模拟"内容相似 → 向量相似"。
 */
@Slf4j
@Primary
@Component
public class MockEmbeddingClient implements EmbeddingClient {

    @Value("${minimax.memory.embedding.dim:384}")
    private int dim;

    @Override
    public String code() { return "mock"; }

    @Override
    public int dim() { return dim; }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return new float[dim];

        // 1) 提取 2-grams 作为特征 (shingle)
        String normalized = text.toLowerCase().replaceAll("\\s+", " ");
        String[] tokens = normalized.split("[\\s,.，。、!?\\-]+");
        long[] features = new long[Math.max(1, tokens.length - 1)];
        for (int i = 0; i < features.length; i++) {
            String shingle = tokens[i] + " " + (i+1 < tokens.length ? tokens[i+1] : "");
            features[i] = stableHash(shingle);
        }

        // 2) 把特征 hash 投射到 dim 维向量
        float[] vec = new float[dim];
        Random r = new Random(0xC0FFEE);
        // 基底随机向量
        for (int i = 0; i < dim; i++) vec[i] = (float) r.nextGaussian();

        // 3) 每个特征对基底做"激活"（使相似文本激活相同维度）
        for (long f : features) {
            int idx = (int) (Math.abs(f) % dim);
            vec[idx] += 1.0f;
        }

        // 4) 归一化
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
        } catch (Exception e) {
            return s.hashCode();
        }
    }
}
