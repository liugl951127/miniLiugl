package com.minimax.monitor.service;

import com.minimax.monitor.collector.MetricsCollector;
import com.minimax.monitor.entity.MetricSnapshot;
import com.minimax.monitor.health.HealthDetailService;
import com.minimax.monitor.mapper.MetricSnapshotMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 指标快照服务。
 * - 启动时初始化 MetricsCollector 的 Counter
 * - 每 60s 把关键指标落库 (metric_snapshot)
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SnapshotService {

    private final MetricsCollector collector;
    private final MetricSnapshotMapper mapper;
    private final HealthDetailService health;

    @PostConstruct
    public void init() {
        collector.init();
        log.info("SnapshotService initialized");
    }

    /** 每 60 秒采一次 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void snapshot() {
        try {
            // 系统指标 (从 HealthDetail 拿)
            Map<String, Object> jvm = health.checkJvm();
            saveSnap("monitor", "jvm_heap_usage",
                    ((Number) jvm.get("usagePercent")).doubleValue(), null);
            saveSnap("monitor", "jvm_heap_used_mb",
                    ((Number) ((Map<?,?>) jvm.get("heap")).get("usedMB")).doubleValue(), null);

            Map<String, Object> disk = health.checkDisk();
            saveSnap("monitor", "disk_usage",
                    ((Number) disk.get("usagePercent")).doubleValue(), null);

            // 业务指标 (从 Collector 拿)
            saveSnap("monitor", "chat_messages_total", collector.getChatMessages(), null);
            saveSnap("monitor", "tool_calls_total", collector.getToolCalls(), null);
            saveSnap("monitor", "rag_queries_total", collector.getRagQueries(), null);
            saveSnap("monitor", "llm_tokens_total", collector.getLlmTokens(), null);
            saveSnap("monitor", "http_5xx_total", collector.getHttp5xx(), null);
            saveSnap("monitor", "active_sessions", collector.getActiveSessions(), null);
            saveSnap("monitor", "kb_count", collector.getKbCount(), null);
            saveSnap("monitor", "user_count", collector.getUserCount(), null);

            log.debug("snapshot saved");
        } catch (Exception e) {
            log.warn("snapshot fail: {}", e.getMessage());
        }
    }

    public void saveSnap(String service, String metricName, double value, String tags) {
        MetricSnapshot s = new MetricSnapshot();
        s.setService(service);
        s.setMetricName(metricName);
        s.setMetricValue(BigDecimal.valueOf(value));
        s.setTags(tags);
        try {
            mapper.insert(s);
        } catch (Exception e) {
            log.warn("saveSnap {} {}: {}", service, metricName, e.getMessage());
        }
    }

    public List<Map<String, Object>> trend(String metricName, String service, int sinceMinutes) {
        return mapper.aggregate(metricName, service, sinceMinutes);
    }

    public List<MetricSnapshot> recent(String metricName, String service, int sinceMinutes, int limit) {
        if (limit <= 0 || limit > 1000) limit = 100;
        return mapper.selectRecent(metricName, service, sinceMinutes, limit);
    }

    /** 每天清理 30 天前的快照 */
    @Scheduled(fixedDelay = 86_400_000, initialDelay = 60_000)
    public void cleanup() {
        try {
            int n = mapper.deleteOlderThan(30);
            log.info("cleanup metric_snapshot: deleted {} rows", n);
        } catch (Exception e) {
            log.warn("cleanup fail: {}", e.getMessage());
        }
    }
}
