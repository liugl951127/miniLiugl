package com.minimax.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MiniMax 自研 AI 服务 (V2.5)
 *
 * 不依赖任何外部大模型 (OpenAI/Claude/DeepSeek)
 * 纯 Java 实现 Transformer 简化版 + 中文分词 + 推理引擎
 *
 * 功能:
 *   - 文本生成 (字符级)
 *   - 文本分类 (情感分析)
 *   - Embedding 向量化 (RAG 用)
 *   - 相似度计算
 *   - 中文分词
 *   - AI 工具配置 (DATA_CLEAN / DATA_ANALYZE / CODE_GEN / SQL / CHAT)
 *   - 多数据源管理 (MySQL/PostgreSQL/Oracle/SQLServer/Mongo/ClickHouse)
 *   - 数据分析 (stats/trend/anomaly/distribution/dedup)
 *   - 项目代码生成 (spring-boot/vue/react/python-flask/node-express)
 *
 * 端口: 8094
 */
@EnableDiscoveryClient
@EnableAsync
@SpringBootApplication
@MapperScan({"com.minimax.ai.mapper", "com.minimax.ai.marketplace", "com.minimax.ai.modelmarket", "com.minimax.ai.template", "com.minimax.ai.webhook"})
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
