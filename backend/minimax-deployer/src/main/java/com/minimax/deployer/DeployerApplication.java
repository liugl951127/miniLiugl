package com.minimax.deployer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Agent Forge 部署服务 - V2.0
 *
 * 智能体群流水线后端:
 *  1. 需求解析 (RequirementsParser)  - LLM 驱动, 提取项目元数据 + 推荐智能体
 *  2. 镜像生成 (ImageBuilder)         - 基于 Freemarker 模板, 为每个智能体生成 Dockerfile
 *  3. 远程部署 (RemoteDeployer)       - 4 目标: Docker / K8s / 云厂商 / 边缘设备
 *  4. 实时监控 (DeploymentMonitor)    - SSE 推送部署状态
 *  5. 版本管理 (ReleaseManager)       - 语义化版本 + 一键回滚
 *
 * 端口: 9010 (V2.0 新增, 与现有服务不冲突)
 * 注册: Nacos / minimax-deployer
 *
 * @author MiniMax AI Team
 * @since V2.0
 */
@SpringBootApplication(scanBasePackages = { "com.minimax.deployer", "com.minimax.common" })
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
@MapperScan("com.minimax.deployer.mapper")
public class DeployerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeployerApplication.class, args);
        System.out.println("""

            ╔══════════════════════════════════════════════════════════╗
            ║  🔥 Agent Forge Deployer Service Started                ║
            ║  Port: 9010                                              ║
            ║  Endpoints: /api/v1/forge/*                               ║
            ╚══════════════════════════════════════════════════════════╝
            """);
    }
}
