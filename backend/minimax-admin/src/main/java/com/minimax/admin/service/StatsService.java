package com.minimax.admin.service;

import com.minimax.admin.client.ServiceClient;
import com.minimax.admin.client.ServiceEndpoints;
import com.minimax.admin.entity.AdminAuditLog;
import com.minimax.admin.mapper.AdminAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务统计聚合。
 *
 * 数据源:
 *  - admin_audit_log (本地) — 操作统计
 *  - 其他 6 个服务 — 调用 stats endpoint (如果存在)
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final AdminAuditLogMapper auditMapper;
    private final ServiceClient client;
    private final ServiceEndpoints endpoints;

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

    /** 模型调用统计 — 调 model 服务 (如有 /stats/calls) */
    public Map<String, Object> modelStats() {
        Map<String, Object> r = new LinkedHashMap<>();
        String body = client.get(endpoints.model(), "/api/v1/models/stats");
        if (body != null) {
            r.put("model", body);
        } else {
            r.put("model", "unavailable");
        }
        return r;
    }

    /** 工具调用统计 — 调 function 服务 */
    public Map<String, Object> toolStats() {
        Map<String, Object> r = new LinkedHashMap<>();
        String body = client.get(endpoints.function(), "/api/v1/function/stats");
        if (body != null) {
            r.put("function", body);
        } else {
            r.put("function", "unavailable");
        }
        return r;
    }

    /** Dashboard 摘要 - 一页看到所有关键指标 */
    public Map<String, Object> dashboard() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("ops", opsStats());
        d.put("model", modelStats());
        d.put("tools", toolStats());
        d.put("periods", periodBounds());
        d.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return d;
    }
}
