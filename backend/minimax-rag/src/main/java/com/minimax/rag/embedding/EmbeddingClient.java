package com.minimax.rag.embedding;

/**
 * RAG 自己的 EmbeddingClient 接口（与 memory 模块解耦）。
 * 生产可指向与 memory 同一个实现 (OpenAI 兼容)。
 */
public interface EmbeddingClient {
    String code();
    float[] embed(String text);
    int dim();
}
