package com.minimax.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.mapper.AlertEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警根因知识库服务 (Day 54).
 *
 * <p>同类告警自动关联历史处理记录：
 * <ul>
 *   <li>根据 metricName / ruleName 模糊匹配相似告警</li>
 *   <li>提取已解决告警的处理经验（notes / resolvedBy / duration）</li>
 *   <li>构建 "同类告警 → 历史处理方案" 知识图谱</li>
 *   <li>用于新告警的 RCA 辅助建议</li>
 * </ul>
 *
 * <h3>知识关联策略</h3>
 * <pre>
 * 相似度判断: metricName 关键词匹配 (Token Jaccard)
 *            + severity 相同
 *            + 时间窗口内 (默认 30 天)
 * 历史记录优先级: resolved → acked → firing
 * 经验提取: notes(确认备注) + resolvedBy(处理人) + duration(恢复时长)
 * </pre>
 *
 * <h3>API</h3>
 * <pre>
 * GET  /api/v1/monitor/alerts/rca/knowledge?metricName=...&historyDays=30&limit=10
 * GET  /api/v1/monitor/alerts/rca/similar?currentAlertId=123
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRcaKnowledgeService {

    private final AlertEventMapper alertEventMapper;

    @Value("${minimax.monitor.rca-knowledge.history-days:30}")
    private int historyDays;

    @Value("${minimax.monitor.rca-knowledge.min-similarity:0.4}")
    private double minSimilarity;

    @Value("${minimax.monitor.rca-knowledge.max-results:10}")
    private int maxResults;

    /**
     * 根据指标名查询历史告警知识（已解决的同类告警处理经验）.
     *
     * @param metricName  指标名（支持模糊匹配）
     * @param historyDays 历史窗口天数
     * @param limit       最大返回数
     * @return 历史处理经验列表
     */
    public List<KnowledgeEntry> queryKnowledge(String metricName, Integer historyDays,
                                                 Integer limit) {
        int days = historyDays != null ? historyDays : this.historyDays;
        int max = limit != null ? Math.min(limit, this.maxResults * 2) : this.maxResults;

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AlertEvent> candidates = alertEventMapper.selectList(
                new QueryWrapper<AlertEvent>()
                        .ge("fired_at", since)
                        .in("status", List.of("resolved", "acked"))
                        .orderByDesc("fired_at")
                        .last("LIMIT 500")
        );

        if (metricName == null || metricName.isBlank()) {
            return candidates.stream()
                    .limit(max)
                    .map(this::toKnowledgeEntry)
                    .toList();
        }

        // Token Jaccard 相似度过滤
        String[] targetTokens = tokenize(metricName);
        return candidates.stream()
                .filter(e -> similarity(targetTokens, tokenize(e.getMetricName())) >= minSimilarity)
                .sorted(Comparator
                        .comparing((AlertEvent e) -> !"resolved".equals(e.getStatus()) ? 1 : 0)
                        .thenComparing(Comparator.comparing(AlertEvent::getFiredAt).reversed()))
                .limit(max)
                .map(this::toKnowledgeEntry)
                .toList();
    }

    /**
     * 根据当前告警 ID 查找同类历史告警 + 处理经验.
     *
     * @param alertId      当前告警 ID
     * @param historyDays  历史窗口
     * @param limit        最大返回数
     * @return 相似历史告警列表
     */
    public List<KnowledgeEntry> findSimilar(Long alertId, Integer historyDays, Integer limit) {
        AlertEvent current = alertEventMapper.selectById(alertId);
        if (current == null) {
            log.warn("AlertRcaKnowledge: alert {} not found", alertId);
            return List.of();
        }

        int days = historyDays != null ? historyDays : this.historyDays;
        int max = limit != null ? Math.min(limit, this.maxResults) : this.maxResults;

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AlertEvent> candidates = alertEventMapper.selectList(
                new QueryWrapper<AlertEvent>()
                        .ge("fired_at", since)
                        .ne("id", alertId)
                        .ne("status", "firing")
                        .orderByDesc("fired_at")
                        .last("LIMIT 300")
        );

        // 精确 metricName 匹配优先，其次 severity 匹配
        return candidates.stream()
                .filter(e -> Objects.equals(e.getMetricName(), current.getMetricName())
                        || Objects.equals(e.getRuleName(), current.getRuleName())
                        || Objects.equals(e.getSeverity(), current.getSeverity()))
                .sorted(Comparator
                        .comparing((AlertEvent e) -> !"resolved".equals(e.getStatus()) ? 1 : 0)
                        .thenComparing(Comparator.comparing(AlertEvent::getFiredAt).reversed()))
                .limit(max)
                .map(this::toKnowledgeEntry)
                .toList();
    }

    /**
     * 获取指标名的知识摘要（高频告警 + 平均恢复时长）.
     */
    public Map<String, Object> knowledgeSummary(String metricName, Integer historyDays) {
        List<KnowledgeEntry> entries = queryKnowledge(metricName, historyDays, 100);
        if (entries.isEmpty()) {
            return Map.of(
                    "metricName", metricName != null ? metricName : "全部",
                    "totalRecords", 0,
                    "resolvedCount", 0,
                    "avgDurationSec", 0L,
                    "topSeverity", "UNKNOWN",
                    "commonCauses", List.of(),
                    "knowledgeReady", false
            );
        }

        long resolved = entries.stream().filter(e -> "resolved".equals(e.status)).count();
        double avgDuration = entries.stream()
                .filter(e -> e.duration != null)
                .mapToLong(e -> e.duration)
                .average().orElse(0);

        Map<String, Long> severityCounts = entries.stream()
                .collect(Collectors.groupingBy(e -> e.severity != null ? e.severity : "UNKNOWN", Collectors.counting()));

        String topSeverity = severityCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("UNKNOWN");

        // 从 notes 中提取常见关键词
        List<String> commonCauses = entries.stream()
                .filter(e -> e.notes != null && !e.notes.isBlank())
                .map(e -> extractKeyPhrase(e.notes))
                .filter(Objects::nonNull)
                .limit(5)
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("metricName", metricName != null ? metricName : "全部");
        summary.put("totalRecords", entries.size());
        summary.put("resolvedCount", resolved);
        summary.put("avgDurationSec", Math.round(avgDuration));
        summary.put("topSeverity", topSeverity);
        summary.put("commonCauses", commonCauses);
        summary.put("knowledgeReady", entries.size() >= 3);
        return summary;
    }

    // ============== 内部工具 ==============

    /** Tokenize: 中英文混合分词 */
    private String[] tokenize(String text) {
        if (text == null || text.isBlank()) return new String[0];
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fff]", " ")
                .trim()
                .split("\\s+");
    }

    /** Token Jaccard 相似度 */
    private double similarity(String[] a, String[] b) {
        if (a.length == 0 || b.length == 0) return 0.0;
        Set<String> setA = new HashSet<>(Arrays.asList(a));
        Set<String> setB = new HashSet<>(Arrays.asList(b));
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        double jaccard = (double) intersection.size() / Math.max(setA.size() + setB.size() - intersection.size(), 1);
        // 额外加权: 包含完整匹配的加 boost
        String joinedA = String.join("", a);
        String joinedB = String.join("", b);
        if (joinedB.contains(joinedA) || joinedA.contains(joinedB)) {
            jaccard = Math.min(1.0, jaccard + 0.3);
        }
        return jaccard;
    }

    /** 将 AlertEvent 转为知识条目 */
    private KnowledgeEntry toKnowledgeEntry(AlertEvent e) {
        return new KnowledgeEntry(
                e.getId(),
                e.getMetricName(),
                e.getRuleName(),
                e.getSeverity(),
                e.getStatus(),
                e.getMessage(),
                e.getFiredAt(),
                e.getResolvedAt(),
                e.getAckedAt(),
                e.getDuration(),
                e.getNotes(),
                e.getResolvedBy()
        );
    }

    /** 从备注中提取关键短语 */
    private String extractKeyPhrase(String notes) {
        if (notes == null || notes.length() < 5) return null;
        // 取前 50 字符，去掉尾部不完整词
        String trimmed = notes.trim();
        if (trimmed.length() <= 50) return trimmed;
        int lastSpace = trimmed.lastIndexOf(' ', 50);
        return lastSpace > 10 ? trimmed.substring(0, lastSpace) : trimmed.substring(0, 50);
    }

    // ============== 数据类 ==============

    public record KnowledgeEntry(
            Long alertId,
            String metricName,
            String ruleName,
            String severity,
            String status,
            String message,
            LocalDateTime firedAt,
            LocalDateTime resolvedAt,
            LocalDateTime ackedAt,
            Long duration,
            String notes,
            String resolvedBy
    ) {}
}
