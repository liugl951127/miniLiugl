package com.minimax.analytics.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Analytics Stats API (V1.0)
 *
 * 直接查数据库表统计，不依赖外部 AI 服务。
 * 所有查询都包裹 try-catch，表不存在时返回空数据，前端不报错。
 */
@RestController
@RequestMapping("/api/v1/analytics/stats")
@RequiredArgsConstructor
public class AnalyticsStatsController {

    private final JdbcTemplate jdbc;

    /**
     * GET /api/v1/analytics/stats/overview
     * 返回核心指标：总调用量、今日调用、独立用户数、平均延迟、成功率
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> r = new LinkedHashMap<>();

        try {
            var rows = jdbc.queryForList("SELECT COUNT(*) as cnt FROM request_log WHERE deleted = 0 LIMIT 1");
            r.put("totalCalls", rows.isEmpty() ? 0 : ((Number) rows.get(0).get("cnt")).longValue());
        } catch (Exception e) {
            r.put("totalCalls", 0L);
        }

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            var rows = jdbc.queryForList(
                    "SELECT COUNT(*) as cnt FROM request_log WHERE DATE(created_at) = ? AND deleted = 0", today);
            r.put("todayCalls", rows.isEmpty() ? 0 : ((Number) rows.get(0).get("cnt")).longValue());
        } catch (Exception e) {
            r.put("todayCalls", 0L);
        }

        try {
            var rows = jdbc.queryForList(
                    "SELECT COUNT(DISTINCT user_id) as cnt FROM request_log WHERE user_id IS NOT NULL AND user_id > 0 AND deleted = 0 LIMIT 1");
            r.put("totalUsers", rows.isEmpty() ? 0 : ((Number) rows.get(0).get("cnt")).longValue());
        } catch (Exception e) {
            r.put("totalUsers", 0L);
        }

        // avgLatency / successRate 从 request_log 真实计算
        try {
            var rows = jdbc.queryForList(
                    "SELECT AVG(latency_ms) as avg_lat, SUM(CASE WHEN status >= 400 OR error = 1 THEN 1 ELSE 0 END) as errors, COUNT(*) as total "
                    + "FROM request_log WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND deleted = 0");
            if (!rows.isEmpty()) {
                var row = rows.get(0);
                double avgLat = row.get("avg_lat") != null ? ((Number) row.get("avg_lat")).doubleValue() : 0;
                long errors = row.get("errors") != null ? ((Number) row.get("errors")).longValue() : 0;
                long total = row.get("total") != null ? ((Number) row.get("total")).longValue() : 0;
                r.put("avgLatency", Math.round(avgLat));
                r.put("successRate", total > 0 ? Math.round((1 - (double) errors / total) * 10000) / 100.0 : 0.0);
            } else {
                r.put("avgLatency", 0);
                r.put("successRate", 0);
            }
        } catch (Exception e) {
            r.put("avgLatency", 0);
            r.put("successRate", 0);
        }

        try {
            String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            var rows = jdbc.queryForList(
                    "SELECT COUNT(*) as cnt FROM request_log WHERE DATE(created_at) = ? AND deleted = 0", yesterday);
            r.put("yesterdayCalls", rows.isEmpty() ? 0 : ((Number) rows.get(0).get("cnt")).longValue());
        } catch (Exception e) {
            r.put("yesterdayCalls", 0);
        }
        r.put("weekTrend", Collections.emptyList());

        return Result.ok(r);
    }

    /**
     * GET /api/v1/analytics/stats/trend?days=7
     * 返回近 N 天每日调用量
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            var rows = jdbc.queryForList(
                    "SELECT DATE(created_at) as day, COUNT(*) as cnt "
                            + "FROM request_log "
                            + "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) AND deleted = 0 "
                            + "GROUP BY DATE(created_at) ORDER BY day ASC",
                    days);
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("day", row.get("day"));
                item.put("calls", ((Number) row.get("cnt")).longValue());
                list.add(item);
            }
        } catch (Exception e) {
            // 表不存在，返回空
        }
        return Result.ok(list);
    }

    /**
     * GET /api/v1/analytics/stats/distribution
     * 返回各模型的调用量分布（从 ai_generation_log）
     */
    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> distribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            var rows = jdbc.queryForList(
                    "SELECT model_name as name, COUNT(*) as value "
                            + "FROM ai_generation_log WHERE model_name IS NOT NULL AND model_name != '' "
                            + "GROUP BY model_name ORDER BY value DESC LIMIT 10");
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", row.get("name"));
                item.put("value", ((Number) row.get("value")).longValue());
                list.add(item);
            }
        } catch (Exception e) {
            // 表不存在，返回空
        }
        return Result.ok(list);
    }

    /**
     * GET /api/v1/analytics/stats/top-users
     * 返回调用量最大的用户排行
     */
    @GetMapping("/top-users")
    public Result<List<Map<String, Object>>> topUsers() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            String sql = """
                SELECT user_id as userId, COUNT(*) as calls
                FROM request_log
                WHERE user_id IS NOT NULL AND user_id > 0 AND deleted = 0
                GROUP BY user_id
                ORDER BY calls DESC LIMIT 10
                """;
            var rows = jdbc.queryForList(sql);
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", row.get("userId"));
                item.put("user", "用户-" + row.get("userId"));
                item.put("calls", ((Number) row.get("calls")).longValue());
                list.add(item);
            }
        } catch (Exception e) {
            // 表不存在，返回空
        }
        return Result.ok(list);
    }

    /**
     * GET /api/v1/analytics/stats/success-rate?days=7
     * 返回近 N 天每日成功率趋势
     */
    @GetMapping("/success-rate")
    public Result<List<Map<String, Object>>> successRate(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            var rows = jdbc.queryForList(
                    "SELECT DATE(created_at) as day, "
                            + "COUNT(*) as total, "
                            + "SUM(CASE WHEN status >= 400 OR error = 1 THEN 1 ELSE 0 END) as errors "
                            + "FROM request_log "
                            + "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) AND deleted = 0 "
                            + "GROUP BY DATE(created_at) ORDER BY day ASC",
                    days);
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("day", row.get("day"));
                long total = row.get("total") != null ? ((Number) row.get("total")).longValue() : 0;
                long errors = row.get("errors") != null ? ((Number) row.get("errors")).longValue() : 0;
                double rate = total > 0 ? Math.round((1.0 - (double) errors / total) * 10000) / 100.0 : 0.0;
                item.put("rate", rate);
                item.put("total", total);
                item.put("errors", errors);
                list.add(item);
            }
        } catch (Exception e) {
            // 表不存在，返回空
        }
        return Result.ok(list);
    }

    /**
     * GET /api/v1/analytics/stats/model-trend?days=7
     * 返回近 N 天各模型的每日调用趋势（top 3 模型）
     */
    @GetMapping("/model-trend")
    public Result<List<Map<String, Object>>> modelTrend(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            // 先查 top 3 模型
            var topModels = jdbc.queryForList(
                    "SELECT model_name FROM ai_generation_log "
                            + "WHERE model_name IS NOT NULL AND model_name != '' "
                            + "AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) "
                            + "GROUP BY model_name ORDER BY COUNT(*) DESC LIMIT 3",
                    days);

            if (topModels.isEmpty()) return Result.ok(list);

            List<String> topNames = topModels.stream()
                    .map(m -> (String) m.get("model_name"))
                    .toList();

            // 查每日趋势
            var rows = jdbc.queryForList(
                    "SELECT DATE(created_at) as day, model_name, COUNT(*) as cnt "
                            + "FROM ai_generation_log "
                            + "WHERE model_name IN (" + String.join(",", topNames.stream().map(n -> "'" + n + "'").toList()) + ") "
                            + "AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) "
                            + "GROUP BY DATE(created_at), model_name ORDER BY day ASC",
                    days);

            // 转换成 { day, [modelName]: count } 结构
            Map<String, Map<String, Object>> dayMap = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String day = String.valueOf(row.get("day"));
                String model = (String) row.get("model_name");
                long cnt = row.get("cnt") != null ? ((Number) row.get("cnt")).longValue() : 0;
                dayMap.computeIfAbsent(day, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("day", day);
                    for (String n : topNames) m.put(n, 0L);
                    return m;
                }).put(model, cnt);
            }
            list.addAll(dayMap.values());
        } catch (Exception e) {
            // 表不存在，返回空
        }
        return Result.ok(list);
    }
}
