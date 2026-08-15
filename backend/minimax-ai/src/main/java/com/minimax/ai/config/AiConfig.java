package com.minimax.ai.config;

import com.minimax.ai.model.MiniTransformer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

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

    /**
     * RestTemplate — 用于 AiMultimodalRealController 代理到 minimax-model 真实服务
     */
    @Bean
    public RestTemplate modelRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return new RestTemplate(factory);
    }

    /**
     * RestTemplate — 用于 AiChatRealController 调用 RAG 服务 (V7.0)
     * 独立实例，避免超时互相影响
     */
    @Bean
    public RestTemplate ragRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return new RestTemplate(factory);
    }
}
