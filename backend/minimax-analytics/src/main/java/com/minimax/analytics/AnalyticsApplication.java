package com.minimax.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 数据智能分析模块 (V5.31)
 *
 * 核心能力:
 *   1. 数据库元数据: information_schema 读取 + 数据画像
 *   2. 多格式导入: csv/json/log 解析 + 质量报告
 *   3. NL2SQL: 自然语言 → LLM → SQL → 安全执行
 *   4. 报告: SQL + 数据 → Markdown + ECharts 配置
 *   5. 趋势/异常: 移动平均 / IQR / z-score
 *
 * 端口: 8096
 * 入口: com.minimax.analytics.AnalyticsApplication
 */
@SpringBootApplication(scanBasePackages = {
    "com.minimax.analytics",
    "com.minimax.common"
})
@MapperScan("com.minimax.analytics.mapper")
@EnableAsync
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
