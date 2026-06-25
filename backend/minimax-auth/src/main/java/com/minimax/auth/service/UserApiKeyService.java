package com.minimax.auth.service;

import com.minimax.auth.dto.CreateApiKeyRequest;
import com.minimax.auth.entity.UserApiKey;
import com.minimax.auth.vo.ApiKeyResponse;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户 API Key 服务 (V5.33 Day 18).
 *
 * 功能:
 *   - 创建 Key（生成 UUID 种子 → SHA-256 hash → 返回原始 Key 一次）
 *   - 列表 / 详情 / 禁用 / 删除
 *   - Key 验证（每次请求时校验 hash）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApiKeyService {

    private final com.minimax.auth.mapper.UserApiKeyMapper apiKeyMapper;

    /** Key 前缀 */
    private static final String KEY_PREFIX = "mmx_";

    /** 创建 API Key */
    @Transactional
    public ApiKeyResponse createKey(Long userId, CreateApiKeyRequest req) {
        String rawKey = KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        String keyHash = sha256(rawKey);

        UserApiKey record = new UserApiKey();
        record.setUserId(userId);
        record.setName(req.getName() == null || req.getName().isBlank() ? "我的 Key" : req.getName());
        record.setKeyHash(keyHash);
        record.setKeyPrefix(rawKey.substring(0, 8) + "****");
        record.setScopes(req.getScopes() == null ? "chat:send,chat:stream" : req.getScopes());
        record.setExpiresAt(req.getExpiresAt());
        record.setEnabled(1);
        record.setUseCount(0L);
        apiKeyMapper.insert(record);

        return ApiKeyResponse.builder()
                .id(record.getId())
                .name(record.getName())
                .keyPrefix(record.getKeyPrefix())
                .scopes(record.getScopes())
                .expiresAt(record.getExpiresAt())
                .enabled(record.getEnabled())
                .useCount(0L)
                .createdAt(record.getCreatedAt())
                .rawKey(rawKey)  // 仅创建时返回一次
                .build();
    }

    /** 列出用户的 Key（不返回 rawKey） */
    public List<ApiKeyResponse> listKeys(Long userId) {
        return apiKeyMapper.selectByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 禁用 / 启用 Key */
    @Transactional
    public void toggleStatus(Long userId, Long keyId, boolean enable) {
        UserApiKey key = apiKeyMapper.selectById(keyId);
        if (key == null || !key.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作此 Key");
        }
        key.setEnabled(enable ? 1 : 0);
        apiKeyMapper.updateById(key);
    }

    /** 删除 Key（软删） */
    @Transactional
    public void deleteKey(Long userId, Long keyId) {
        UserApiKey key = apiKeyMapper.selectById(keyId);
        if (key == null || !key.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作此 Key");
        }
        apiKeyMapper.deleteById(keyId);
    }

    /** 轮换 Key（删除旧 → 创建新，返回新 rawKey） */
    @Transactional
    public ApiKeyResponse rotateKey(Long userId, Long keyId, CreateApiKeyRequest req) {
        deleteKey(userId, keyId);
        return createKey(userId, req != null ? req : new CreateApiKeyRequest());
    }

    /**
     * 验证 API Key，返回持有用户 ID。
     * 用于网关 / 过滤器拦截验证。
     */
    public Long validateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return null;
        }
        String hash = sha256(rawKey);
        UserApiKey key = apiKeyMapper.selectByKeyHash(hash);
        if (key == null || key.getEnabled() == 0) {
            return null;
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        // 计数 +1
        apiKeyMapper.incrementUseCount(key.getId());
        return key.getUserId();
    }

    /** 全局 API Key 统计（供管理员） */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = apiKeyMapper.selectStats();
        List<Map<String, Object>> byStatus = apiKeyMapper.selectCountByStatus();

        long activeKeys = 0;
        long inactiveKeys = 0;
        for (Map<String, Object> row : byStatus) {
            Integer enabled = (Integer) row.get("enabled");
            Long cnt = ((Number) row.get("cnt")).longValue();
            if (enabled == 1) activeKeys = cnt;
            else inactiveKeys = cnt;
        }

        return Map.of(
                "totalKeys", stats.get("totalKeys") != null ? ((Number) stats.get("totalKeys")).longValue() : 0L,
                "totalCalls", stats.get("totalCalls") != null ? ((Number) stats.get("totalCalls")).longValue() : 0L,
                "activeKeys", activeKeys,
                "inactiveKeys", inactiveKeys
        );
    }

    // ---- private helpers ----

    private String sha256(String input) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] h = d.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private ApiKeyResponse toResponse(UserApiKey k) {
        return ApiKeyResponse.builder()
                .id(k.getId())
                .name(k.getName())
                .keyPrefix(k.getKeyPrefix())
                .scopes(k.getScopes())
                .expiresAt(k.getExpiresAt())
                .lastUsedAt(k.getLastUsedAt())
                .useCount(k.getUseCount())
                .enabled(k.getEnabled())
                .createdAt(k.getCreatedAt())
                .build();
    }
}
