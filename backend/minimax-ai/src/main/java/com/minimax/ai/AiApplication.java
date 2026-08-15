package com.minimax.ai;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MiniMax 自研 AI 服务 (V2.5)
 *
 * 不依赖任何外部大模型 (OpenAI/Claude/DeepSeek)
 * 纯 Java 实现 Transformer 简化版 + 中文分词 + 推理引擎
 *
 * 端口: 8094
 */
@EnableAsync
@SpringBootApplication
// h2local: 只扫描 ai + common（model service 通过 stub 提供）
@ComponentScan(
    basePackages = {"com.minimax.ai", "com.minimax.common"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.minimax\\.model\\..*")
    }
)
@MapperScan({
    "com.minimax.ai.mapper",
    "com.minimax.ai.marketplace",
    "com.minimax.ai.modelmarket",
    "com.minimax.ai.template",
    "com.minimax.ai.webhook"
})
@ConfigurationPropertiesScan("com.minimax.ai.intent")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
