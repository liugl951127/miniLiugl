package com.minimax.admin.controller;

import com.minimax.admin.mapper.AdminAuditLogMapper;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统计专用控制器 (Dashboard 真实化 V6.8)
 *
 * 接口:
 *   GET /api/v1/admin/stats/hourly    按小时聚合真实调用量
 *   GET /api/v1/admin/stats/services 服务状态列表
 */
@Tag(name = "Dashboard 统计")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // V6.8.2: Dashboard 统计仅 ADMIN 可访问
public class StatsController {

    private final AdminAuditLogMapper auditMapper;

    /**
     * GET /admin/stats/hourly
     * 查 admin_audit_log 按小时聚合真实调用量.
     *
     * @param hours 统计小时数，默认 24，最大 168 (7天)
     */
    @Operation(summary = "小时级调用量趋势")
    @GetMapping("/hourly")
    public Result<List<Map<String, Object>>> hourlyStats(
            @RequestParam(defaultValue = "24") int hours) {
        hours = Math.min(Math.max(hours, 1), 168);
        String since = LocalDateTime.now().minusHours(hours)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            var rows = auditMapper.countByHour(since);
            // 构建 hour → cnt 的 map
            Map<String, Long> hourMap = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Object hourVal = row.get("stat_hour");
                Object cntVal = row.get("cnt");
                if (hourVal != null && cntVal != null) {
                    hourMap.put(String.valueOf(hourVal), ((Number) cntVal).longValue());
                }
            }
            // 补全所有小时，没数据的填 0
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            for (int i = hours - 1; i >= 0; i--) {
                Calendar c = (Calendar) cal.clone();
                c.add(Calendar.HOUR_OF_DAY, -i);
                String hourStr = String.format("%04d-%02d-%02d %02d:00",
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH) + 1,
                        c.get(Calendar.DAY_OF_MONTH),
                        c.get(Calendar.HOUR_OF_DAY));
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("hour", hourStr);
                item.put("calls", hourMap.getOrDefault(hourStr, 0L));
                list.add(item);
            }
        } catch (Exception e) {
            log.warn("[StatsController] hourlyStats 查库失败: {}", e.getMessage());
            // 查库失败时返回全 0 的时间序列
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            for (int i = hours - 1; i >= 0; i--) {
                Calendar c = (Calendar) cal.clone();
                c.add(Calendar.HOUR_OF_DAY, -i);
                String hourStr = String.format("%04d-%02d-%02d %02d:00",
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH) + 1,
                        c.get(Calendar.DAY_OF_MONTH),
                        c.get(Calendar.HOUR_OF_DAY));
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("hour", hourStr);
                item.put("calls", 0L);
                list.add(item);
            }
        }
        return Result.ok(list);
    }

    /**
     * GET /admin/stats/services
     * 返回平台微服务状态列表.
     * 当前硬编码 UNKNOWN；后续接入 Nacos / K8s 健康检查可替换.
     */
    @Operation(summary = "微服务状态列表")
    @GetMapping("/services")
    public Result<List<Map<String, Object>>> serviceStatus() {
        String[] names = {
                "minimax-ai",
                "minimax-auth",
                "minimax-chat",
                "minimax-gateway",
                "minimax-model",
                "minimax-pipeline",
                "minimax-monitor"
        };
        List<Map<String, Object>> list = new ArrayList<>();
        for (String n : names) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", n);
            s.put("status", "UNKNOWN");
            list.add(s);
        }
        return Result.ok(list);
    }
}
