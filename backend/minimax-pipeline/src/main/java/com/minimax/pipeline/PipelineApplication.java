package com.minimax.pipeline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;


/**
 * 画布工作流数据分析模块 (V6.8.1)
 *
 * 端口 8091. 提供:
 *   1. 13 种节点类型: 3 个 INPUT + 8 个 TRANSFORM + 2 个 OUTPUT
 *   2. DAG 拓扑校验 (Kahn 算法, 环检测)
 *   3. 异步执行引擎 (节点状态: PENDING/RUNNING/SUCCESS/FAILED)
 *   4. 工作流版本管理 (V5.32.x)
 *   5. 函数工具调用 (function_ext 子模块)
 *
 * V6.8.1 重构:
 *   - 自建 AnalyticsDataSourceMapper 直接查 analytics_datasource 表（解耦 minimax-analytics）
 *   - 支持通过 Feign 调用 minimax-model 进行 LLM 增强
 */
@SpringBootApplication(scanBasePackages = {
        "com.minimax.pipeline",
        "com.minimax.pipeline.function_ext",
        "com.minimax.common"
})
@MapperScan({
        "com.minimax.pipeline.mapper",
        "com.minimax.pipeline.function_ext.mapper"
})
@EnableAsync
@EnableFeignClients
public class PipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineApplication.class, args);
    }
}
