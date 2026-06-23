package com.minimax.auth.controller;

import com.minimax.auth.service.UserApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API Key 内部验证接口 (V5.33 Day 19).
 *
 * 供网关 Gateway 调用，用于验证外部用户的 Bearer mmx_xxxx Token。
 * 仅内网访问，不对外暴露。
 *
 * POST /internal/apikey/validate
 * Body: { "rawKey": "mmx_a1b2c3..." }
 * Response: { "userId": 123 }  或  { "error": "invalid_key" }
 */
@Slf4j
@RestController
@RequestMapping("/internal/apikey")
@RequiredArgsConstructor
@Tag(name = "内部 API Key 验证")
public class ApiKeyInternalController {

    private final UserApiKeyService apiKeyService;

    @Operation(summary = "验证 API Key，返回对应用户 ID")
    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody Map<String, String> body) {
        String rawKey = body.get("rawKey");
        if (rawKey == null || rawKey.isBlank()) {
            return Map.of("error", "missing_key");
        }

        Long userId = apiKeyService.validateKey(rawKey);
        if (userId == null) {
            log.debug("API Key 验证失败: rawKey=***{}", rawKey.substring(Math.max(0, rawKey.length() - 6)));
            return Map.of("error", "invalid_key");
        }

        log.debug("API Key 验证成功: userId={}", userId);
        return Map.of("userId", userId);
    }
}
