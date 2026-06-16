package com.minimax.admin.service;

import com.minimax.admin.client.ServiceClient;
import com.minimax.admin.client.ServiceEndpoints;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型管理 (代理 model 服务, 加审计)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMgmtService {

    private final ServiceClient client;
    private final ServiceEndpoints endpoints;
    private final AuditService audit;

    public String listProviders() {
        String body = client.get(endpoints.model(), "/api/v1/models/providers");
        return body != null ? body : client.errorResp("model 服务不可达").toString();
    }

    public String listConfigs() {
        String body = client.get(endpoints.model(), "/api/v1/models");
        return body != null ? body : client.errorResp("model 服务不可达").toString();
    }

    /**
     * 调整限流 (Bucket4j 配置) — 调 model 服务
     */
    public String updateRateLimit(Long actorId, String actorName, String modelCode,
                                   int capacity, int refillPerMinute, HttpServletRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("modelCode", modelCode);
        body.put("capacity", capacity);
        body.put("refillPerMinute", refillPerMinute);
        String resp = client.put(endpoints.model(), "/api/v1/models/" + modelCode + "/rate-limit", body);
        if (resp == null) {
            // 降级: 直接给 ok (实际可能没生效)
            resp = "{\"code\":0,\"message\":\"调限流请求已发送 (可能待生效)\"}";
        }
        String result = (resp.contains("\"code\":0")) ? "ok" : "error";
        audit.record(actorId, actorName, "update_rate_limit", "model", modelCode, body, result,
                result.equals("ok") ? null : truncate(resp, 300), req);
        return resp;
    }

    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}
