package com.minimax.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.admin.client.ServiceClient;
import com.minimax.admin.client.ServiceEndpoints;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理 (代理 auth 服务, 加审计)。
 * auth 服务的具体 API 路径:
 *   GET    /api/v1/auth/users              列表
 *   GET    /api/v1/auth/users/{id}         详情
 *   POST   /api/v1/auth/register           注册
 *   POST   /api/v1/auth/reset-password     重置密码 (假设)
 *   POST   /api/v1/auth/users/{id}/disable 启停 (假设)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMgmtService {

    private final ServiceClient client;
    private final ServiceEndpoints endpoints;
    private final AuditService audit;
    private final ObjectMapper json = new ObjectMapper();

    public String listUsers(int page, int size) {
        String body = client.get(endpoints.auth(),
                "/api/v1/auth/users?page=" + page + "&size=" + size);
        return body != null ? body : client.errorResp("auth 服务不可达").toString();
    }

    public String getUser(Long id) {
        String body = client.get(endpoints.auth(), "/api/v1/auth/users/" + id);
        return body != null ? body : client.errorResp("auth 服务不可达").toString();
    }

    public String createUser(Long actorId, String actorName, Map<String, Object> body, HttpServletRequest req) {
        String resp = client.post(endpoints.auth(), "/api/v1/auth/register", body);
        String result = (resp != null && resp.contains("\"code\":0")) ? "ok" : "error";
        audit.record(actorId, actorName, "create_user", "user", null, body, result,
                result.equals("ok") ? null : truncate(resp, 300), req);
        return resp != null ? resp : client.errorResp("auth 服务不可达").toString();
    }

    /**
     * 重置密码 — 调 auth 服务专用端点
     */
    public String resetPassword(Long actorId, String actorName, Long userId, String newPassword, HttpServletRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("newPassword", newPassword);
        String resp = client.post(endpoints.auth(), "/api/v1/auth/admin/reset-password", body);
        // 兼容: 端点可能不存在 → 降级
        if (resp == null) {
            resp = "{\"code\":1500,\"message\":\"重置密码端点不可用\"}";
        }
        String result = (resp.contains("\"code\":0")) ? "ok" : "error";
        audit.record(actorId, actorName, "reset_password", "user", String.valueOf(userId), body, result,
                result.equals("ok") ? null : truncate(resp, 300), req);
        return resp;
    }

    /**
     * 启停用户 — 调 auth 服务专用端点
     */
    public String toggleUser(Long actorId, String actorName, Long userId, boolean enable, HttpServletRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", enable);
        String resp = client.put(endpoints.auth(), "/api/v1/auth/users/" + userId + "/status", body);
        if (resp == null) {
            resp = "{\"code\":1500,\"message\":\"启停端点不可用\"}";
        }
        String result = (resp.contains("\"code\":0")) ? "ok" : "error";
        audit.record(actorId, actorName, enable ? "enable_user" : "disable_user",
                "user", String.valueOf(userId), body, result,
                result.equals("ok") ? null : truncate(resp, 300), req);
        return resp;
    }

    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}
