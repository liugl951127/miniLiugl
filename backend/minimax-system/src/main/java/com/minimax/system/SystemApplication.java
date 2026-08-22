package com.minimax.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MiniMax System 微服务启动入口 (T-system-module).
 *
 * 职责:
 *   - 前端左侧菜单
 *   - 平台信息查询
 *   - 公告列表
 *   - 健康检查
 *   - 心跳 (ping/pong)
 *
 * 端口: 9086
 * 路由: gateway 转发 /api/v1/system/** → lb://minimax-system
 */
@SpringBootApplication(scanBasePackages = {
        "com.minimax.system",
        "com.minimax.common"
})
public class SystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
