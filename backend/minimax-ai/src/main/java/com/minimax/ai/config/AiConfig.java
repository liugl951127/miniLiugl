package com.minimax.ai.config;

import com.minimax.ai.model.MiniTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模块配置 (V2.8.3)
 * 解决: SimpleEmbedding 需要 MiniTransformer bean
 */
@Configuration
public class AiConfig {

    /**
     * Mini Transformer 自研模型
     * 词表/隐藏维度/头数/层数/最大序列
     */
    @Bean
    public MiniTransformer miniTransformer() {
        return new MiniTransformer(8192, 128, 4, 2, 128);
    }
}
