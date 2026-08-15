package com.minimax.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.admin.client.ServiceClient;
import com.minimax.admin.client.ServiceEndpoints;
import com.minimax.admin.mapper.AdminAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务统计聚合 (V6.8.2 真实数据).
 *
 * <p>V6.8.2: dashboard() 不再硬编码 totalUsers=0/errorRate=0%，
 * 改为 HTTP 调用 auth 服务获取真实用户数据。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final AdminAuditLogMapper auditMapper;
    private final ServiceClient client;
    private final ServiceEndpoints endpoints;
    private final ObjectMapper json = new ObjectMapper();

    /** 今日 / 本周 / 本月 边界 */
    public Map<String, String> periodBounds() {
        LocalDateTime now = LocalDateTime.now();
        String today = now.toLocalDate().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String weekStart = now.minusDays(7).toLocalDate().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String monthStart = now.minusDays(30).toLocalDate().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("today", today);
        m.put("week7d", weekStart);
        m.put("month30d", monthStart);
        return m;
    }

    /** 操作统计 — 按 action */
    public Map<String, Object> opsStats() {
        Map<String, String> bounds = periodBounds();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("today", auditMapper.countByAction(bounds.get("today")));
        r.put("last7d", auditMapper.countByAction(bounds.get("week7d")));
        r.put("last30d", auditMapper.countByAction(bounds.get("month30d")));
        r.put("byResourceType", auditMapper.countByResourceType(bounds.get("week7d")));
        return r;
    }

    /** 模型调用统计 — 返回在线模型数量 (V6.8.2 修复字段名) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> modelStats(HttpServletRequest req) {
        Map<String, Object> r = new LinkedHashMap<>();
        try {
            String body = client.get(endpoints.model(), "/api/v1/models", jwtFrom(req));
            if (body != null) {
                var list = json.readValue(body, java.util.List.class);
                r.put("count", list.size());
                r.put("models", list.size());  // 前端期望的字段名
            } else {
                r.put("count", 0);
                r.put("models", 0);
            }
        } catch (Exception ex) {
            r.put("count", 0);
            r.put("models", 0);
        }
        return r;
    }

    /** 工具调用统计 — 调 function 服务 */
    public Map<String, Object> toolStats(HttpServletRequest req) {
        Map<String, Object> r = new LinkedHashMap<>();
        String body = client.get(endpoints.function(), "/api/v1/function/stats", jwtFrom(req));
        if (body != null) {
            r.put("function", body);
        } else {
            r.put("function", "unavailable");
        }
        return r;
    }

    /** Dashboard 摘要 - 一页看到所有关键指标 (V6.8.2 真实数据) */
    public Map<String, Object> dashboard(HttpServletRequest req) {
        Map<String, Object> d = new LinkedHashMap<>();
        String jwt = jwtFrom(req);

        // 真实用户总数 + 今日登录 (HTTP 调用 auth 服务)
        try {
            String body = client.get(endpoints.auth(), "/api/v1/auth/stats", jwt);
            if (body != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> authStats = json.readValue(body, Map.class);
                d.put("totalUsers", authStats.getOrDefault("totalUsers", 0L));
                d.put("todayLogins", authStats.getOrDefault("todayLogins", 0L));
                // V6.8.2: 前端 Dashboard 期望 users/sessions/calls/models
                d.put("users", authStats.getOrDefault("totalUsers", 0L));
            } else {
                d.put("totalUsers", 0L);
                d.put("todayLogins", 0L);
            }
        } catch (Exception ex) {
            d.put("totalUsers", 0L);
            d.put("todayLogins", 0L);
        }

        // 活跃会话数 (今日操作记录数)
        try {
            var ops = opsStats();
            d.put("activeSessions", ops.getOrDefault("today", 0L));
            d.put("apiCalls", ops.getOrDefault("today", 0L));
            // V6.8.2: 前端 Dashboard 期望 sessions/calls
            d.put("sessions", ops.getOrDefault("today", 0L));
            d.put("calls", ops.getOrDefault("today", 0L));
        } catch (Exception ex) {
            d.put("activeSessions", 0L);
            d.put("apiCalls", 0L);
        }

        // 真实错误率 (今日失败/总操作)
        String errorRate = "0%";
        try {
            Map<String, String> bounds = periodBounds();
            var todayActions = auditMapper.countByAction(bounds.get("today"));
            long total = 0, failed = 0;
            for (var row : todayActions) {
                Object cnt = row.get("cnt");
                long count = cnt instanceof Number ? ((Number) cnt).longValue() : 0;
                total += count;
                String result = (String) row.get("result");
                if (result != null && (result.startsWith("error") || result.startsWith("exception"))) {
                    failed += count;
                }
            }
            if (total > 0) {
                double rate = (double) failed / total * 100;
                errorRate = String.format("%.1f%%", rate);
            }
        } catch (Exception ex) {
            // 降级
        }
        d.put("errorRate", errorRate);

        // 完整 ops 统计
        d.put("opsStats", opsStats());
        d.put("model", modelStats(req));
        // V6.8.2: 前端 Dashboard 期望 models (数字)
        var modelData = modelStats(req);
        d.put("models", modelData.getOrDefault("models", 0));
        d.put("tools", toolStats(req));
        d.put("periods", periodBounds());
        d.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return d;
    }

    private String jwtFrom(HttpServletRequest req) {
        if (req == null) return null;
        String h = req.getHeader("Authorization");
        return (h != null && !h.isBlank()) ? h : null;
    }
}
