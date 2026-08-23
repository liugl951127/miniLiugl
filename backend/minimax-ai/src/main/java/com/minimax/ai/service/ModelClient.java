package com.minimax.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.minimax.common.feign.model.ChatRequestDTO;
import com.minimax.common.feign.model.ChatResponseDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * AI 模块 → Model 模块 HTTP 客户端 (V6.8.1 重构)
 *
 * V7.0: 通过 RestTemplate 远程调用 minimax-model 服务的 /api/v1/models/chat 接口。
 * V6.8.1: 改用 common.feign.model 中的共享 DTO，彻底解耦 Maven 依赖。
 */
@Slf4j
@Service
public class ModelClient {

    private final RestTemplate modelRestTemplate;

    @Value("${minimax.model.service-url:http://minimax-model:8084}")
    private String modelServiceUrl;

    @Value("${minimax.jwt.secret}")
    private String jwtSecret;

    @Value("${minimax.jwt.issuer:minimax-platform}")
    private String jwtIssuer;

    public ModelClient() {
        this.modelRestTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(120_000))
                .build();
    }

    /**
     * 同步调用 Model 服务进行对话
     *
     * @param userId 用户ID
     * @param req    对话请求
     * @return ChatResponseDTO，失败返回 null
     */
    public ChatResponseDTO chat(Long userId, ChatRequestDTO req) {
        try {
            String url = modelServiceUrl + "/api/v1/models/internal/chat?userId=" + (userId != null ? userId : 0);
            log.debug("[ModelClient] 调用 Model 服务: url={}, model={}", url, req.getModel());

            // 构建请求体
            JSONObject body = JSONObject.from(req);

            // 生成内部服务 JWT Token
            String internalToken = generateInternalToken(userId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(internalToken);
            HttpEntity<JSONObject> entity = new HttpEntity<>(body, headers);

            // 发送 POST 请求（携带认证 Token）
            String resp = modelRestTemplate.postForObject(url, entity, String.class);
            if (resp == null) {
                log.warn("[ModelClient] Model 服务返回空");
                return null;
            }

            // 解析 Result<ChatResponseDTO> 响应
            JSONObject result = JSON.parseObject(resp);
            if (result.getIntValue("code") != 0) {
                log.warn("[ModelClient] Model 服务调用失败: code={}, msg={}",
                        result.getIntValue("code"), result.getString("message"));
                return null;
            }

            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                log.warn("[ModelClient] Model 服务返回 data 为空");
                return null;
            }

            ChatResponseDTO response = data.toJavaObject(ChatResponseDTO.class);
            log.debug("[ModelClient] Model 服务调用成功, content长度={}",
                    response.getContent() != null ? response.getContent().length() : 0);
            return response;

        } catch (Exception e) {
            log.error("[ModelClient] Model 服务调用异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取所有已启用的模型列表（含自研 + 云端）。
     * 供 SmartModelRouter 构建候选模型池。
     */
    public List<Map<String, Object>> listEnabledModels() {
        try {
            String url = modelServiceUrl + "/api/v1/models/enabled";
            String resp = modelRestTemplate.getForObject(url, String.class);
            if (resp == null) return List.of();
            JSONObject result = JSON.parseObject(resp);
            if (result.getIntValue("code") != 0) return List.of();
            Object data = result.get("data");
            if (data instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?>) out.add((Map<String, Object>) item);
                }
                return out;
            }
        } catch (Exception e) {
            log.warn("[ModelClient] 获取启用模型列表失败: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * 获取本地/自研模型列表（含 ONNX）。
     * 调用 minimax-model 的 /api/v1/models/local/providers
     */
    public List<Map<String, Object>> listLocalProviders() {
        try {
            String url = modelServiceUrl + "/api/v1/models/local/providers";
            String resp = modelRestTemplate.getForObject(url, String.class);
            if (resp == null) return List.of();
            JSONObject result = JSON.parseObject(resp);
            if (result.getIntValue("code") != 0) return List.of();
            Object data = result.get("data");
            if (data instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?>) out.add((Map<String, Object>) item);
                }
                return out;
            }
        } catch (Exception e) {
            log.warn("[ModelClient] 获取本地模型列表失败: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * 生成内部服务调用的 JWT Token
     */
    private String generateInternalToken(Long userId) {
        try {
            byte[] raw = jwtSecret.getBytes(StandardCharsets.UTF_8);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw);
            SecretKey key = Keys.hmacShaKeyFor(hash);
            long uid = userId != null ? userId : 0;
            return Jwts.builder()
                    .subject(String.valueOf(uid))
                    .claim("uname", "internal-service")
                    .claim("roles", List.of("SUPER_ADMIN"))
                    .issuer(jwtIssuer)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            log.error("[ModelClient] 生成内部 Token 失败: {}", e.getMessage());
            return "";
        }
    }
}
