package com.minimax.memory.embedding;

/**
 * Embedding 客户端接口。
 * 任何兼容 OpenAI / MiniMax-M3 Embeddings API 的供应商都实现此接口。
 */
public interface EmbeddingClient {

    /** provider code: openai / minimax / mock。 */
    String code();

    /**
     * 把单条文本转成向量。
     * @return 向量 (float[]) - 长度 = dim()
     */
    float[] embed(String text);

    /** 向量维度。 */
    int dim();
}
