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
 *
 * <p>V3.5.25 修复:</p>
 * <ul>
 *   <li>listUsers 用 getWithQuery, query 参数 URL encoded</li>
 *   <li>所有调用传 caller JWT (authHeader 透传), 避免 401</li>
 *   <li>ServiceEndpoints 端口已修正 (memory→chat, function→pipeline)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMgmtService {

    private final ServiceClient client;
    private final ServiceEndpoints endpoints;
    private final AuditService audit;
    private final ObjectMapper json = new ObjectMapper();

    public String listUsers(int page, int size, HttpServletRequest req) {
        Map<String, String> q = new HashMap<>();
        q.put("page", String.valueOf(page));
        q.put("size", String.valueOf(size));
        String body = client.getWithQuery(endpoints.auth(), "/api/v1/auth/users", q, jwtFrom(req));
        return body != null ? body : client.errorResp("auth 服务不可达").toString();
    }

    public String getUser(Long id, HttpServletRequest req) {
        String body = client.get(endpoints.auth(), "/api/v1/auth/users/" + id, jwtFrom(req));
        return body != null ? body : client.errorResp("auth 服务不可达").toString();
    }

    public String createUser(Long actorId, String actorName, Map<String, Object> body, HttpServletRequest req) {
        String resp = client.post(endpoints.auth(), "/api/v1/auth/register", body, jwtFrom(req));
        String result = (resp != null && resp.contains("\"code\":0")) ? "ok" : "error";
        audit.record(actorId, actorName, "create_user", "user", null, body, result,
                result.equals("ok") ? null : truncate(resp, 300), req);
        return resp != null ? resp : client.errorResp("auth 服务不可达").toString();
    }

    public String resetPassword(Long actorId, String actorName, Long userId, String newPassword, HttpServletRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("newPassword", newPassword);
        String resp = client.post(endpoints.auth(), "/api/v1/auth/admin/reset-password", body, jwtFrom(req));
        if (resp == null) {
            resp = "{\"code\":1500,\"message\":\"重置密码端点不可用\"}";
        }
        String result = (resp.contains("\"code\":0")) ? "ok" : "error";
        audit.record(actorId, actorName, "reset_password", "user", String.valueOf(userId), body, result,
                result.equals("ok") ? null : truncate(resp, 300), req);
        return resp;
    }

    public String toggleUser(Long actorId, String actorName, Long userId, boolean enable, HttpServletRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", enable);
        String resp = client.put(endpoints.auth(), "/api/v1/auth/users/" + userId + "/status", body, jwtFrom(req));
        if (resp == null) {
            resp = "{\"code\":1500,\"message\":\"启停端点不可用\"}";
        }
        String result = (resp.contains("\"code\":0")) ? "ok" : "error";
        audit.record(actorId, actorName, enable ? "enable_user" : "disable_user",
                "user", String.valueOf(userId), body, result,
                result.equals("ok") ? null : truncate(resp, 300), req);
        return resp;
    }

    /**
     * 提取调用方 JWT (V3.5.25+)
     * 优先级: 1) Authorization header, 2) null
     */
    private String jwtFrom(HttpServletRequest req) {
        if (req == null) return null;
        String h = req.getHeader("Authorization");
        return (h != null && !h.isBlank()) ? h : null;
    }

    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}
