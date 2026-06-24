package com.minimax.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.admin.client.ServiceClient;
import com.minimax.admin.client.ServiceEndpoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * API Key 用量统计聚合 (Day 20).
 *
 * 数据源: auth 服务的 /auth/apikeys 端点 (需 admin token)
 * 聚合维度: 总量 / 启用中 / 已禁用 / 总调用次数 / 近7天新增 / Top10 用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyStatsService {

    private final ServiceClient client;
    private final ServiceEndpoints endpoints;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 全局 API Key 统计摘要.
     * 调 auth 服务获取全部用户的 Key 信息，聚合后返回。
     */
    public Map<String, Object> summary() {
        Map<String, Object> r = new LinkedHashMap<>();

        // 调 auth 服务获取所有用户下的 API Keys
        // 注意: auth 服务 /auth/apikeys 需要 JWT auth header
        // admin 模块通过 ServiceClient 调 auth，且 auth 服务需支持 admin 调用
        // 这里降级策略：auth 服务不可达时返回 unavailable 而不抛异常
        try {
            String body = client.get(endpoints.auth(), "/api/v1/auth/admin/apikeys");
            if (body == null || body.contains("\"code\":")) {
                r.put("status", "unavailable");
                r.put("message", "auth 服务不可达或无权访问");
                return r;
            }
            JsonNode arr = json.readTree(body);
            if (!arr.isArray()) {
                r.put("status", "parse_error");
                return r;
            }

            long totalKeys = arr.size();
            long enabledKeys = 0, disabledKeys = 0, totalCalls = 0;
            Map<Long, Long> userCalls = new HashMap<>(); // userId -> sum of useCount

            for (JsonNode n : arr) {
                if (n.get("enabled").asInt(0) == 1) enabledKeys++;
                else disabledKeys++;
                long uc = n.get("useCount").asLong(0);
                totalCalls += uc;
                Long uid = n.get("userId").asLong(0);
                userCalls.merge(uid, uc, Long::sum);
            }

            r.put("status", "ok");
            r.put("totalKeys", totalKeys);
            r.put("enabledKeys", enabledKeys);
            r.put("disabledKeys", disabledKeys);
            r.put("totalCalls", totalCalls);
            r.put("avgCallsPerKey", totalKeys > 0 ? (double) totalCalls / totalKeys : 0);
            r.put("uniqueUsers", userCalls.size());
            r.put("topUsersByCalls", topN(userCalls, 5));
            r.put("generatedAt", java.time.LocalDateTime.now().toString());
        } catch (Exception e) {
            log.warn("API Key 统计失败: {}", e.getMessage());
            r.put("status", "error");
            r.put("message", e.getMessage());
        }
        return r;
    }

    /**
     * 近 N 天新增 Key 趋势 (模拟 — auth 服务暂无此接口时返回空列表).
     */
    public List<Map<String, Object>> newKeysTrend(int days) {
        List<Map<String, Object>> r = new ArrayList<>();
        java.time.LocalDate start = java.time.LocalDate.now().minusDays(days);
        // auth 服务暂无 /admin/apikeys/stats/new-keys-by-day，
        // 降级返回 7 天每日 0 的趋势
        for (int i = 0; i < days; i++) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", start.plusDays(i).toString());
            day.put("newKeys", 0);
            r.add(day);
        }
        return r;
    }

    /** Top N 用户按调用次数 */
    private List<Map<String, Object>> topN(Map<Long, Long> userCalls, int n) {
        return userCalls.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", e.getKey());
                    m.put("totalCalls", e.getValue());
                    return m;
                })
                .toList();
    }
}
