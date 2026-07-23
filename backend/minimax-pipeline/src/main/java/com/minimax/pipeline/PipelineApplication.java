package com.minimax.pipeline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 画布工作流数据分析模块 (V5.32)
 *
 * 端口 8097. 提供:
 *   1. 13 种节点类型: 3 个 INPUT + 8 个 TRANSFORM + 2 个 OUTPUT
 *   2. DAG 拓扑校验 (Kahn 算法, 环检测)
 *   3. 异步执行引擎 (节点状态: PENDING/RUNNING/SUCCESS/FAILED)
 *   4. 工作流版本管理 (V5.32.x)
 *   5. 与 minimax-analytics 的 ReportService 集成
 */
@SpringBootApplication(scanBasePackages = {
        "com.minimax.pipeline",
        "com.minimax.pipeline.function_ext",
        "com.minimax.common",
        "com.minimax.analytics",
        "com.minimax.model"
})
@MapperScan({"com.minimax.pipeline.mapper", "com.minimax.pipeline.function_ext.mapper"})
@EnableAsync
public class PipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineApplication.class, args);
    }
}
