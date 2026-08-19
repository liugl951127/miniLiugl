package com.minimax.agent.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign 客户端：agent → auth API Key 验证
 *
 * V6.8.1: 替代 Maven 编译依赖 minimax-auth。
 * 路由: POST /internal/apikey/validate → lb://minimax-auth
 */
@FeignClient(
        name = "minimax-auth",
        contextId = "authApiKeyClient",
        path = "/internal/apikey"
)
public interface AuthApiKeyClient {

    /**
     * 验证 API Key，返回对应用户 ID
     * POST /internal/apikey/validate
     * Body: { "rawKey": "mmx_a1b2c3..." }
     */
    @PostMapping("/validate")
    Map<String, Object> validate(@RequestBody Map<String, String> body);
}
