package com.minimax.monitor.service;

import com.minimax.monitor.entity.AlertEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能告警根因分析服务 (Day 30).
 *
 * <p>告警触发后，通过 LLM 推理可能根因 + 建议操作，
 * 替代传统的逐一排查方式，大幅缩短 MTTR（Mean Time To Resolve）。
 *
 * <h3>RCA 分析流程</h3>
 * <pre>
 * AlertEvent → 构建上下文 → LLM 推理 → RCA Result
 *   根因类别: 资源瓶颈 / 配置错误 / 外部依赖 / 代码 bug / 流量突增
 *   建议操作: 重启 / 扩容 / 回滚 / 降级 / 人工介入
 * </pre>
 *
 * <h3>根因分类</h3>
 * <table>
 *   <tr><th>类别</th><th>关键词</th></tr>
 *   <tr><td>RESOURCE_BOTTLENECK</td><td>CPU/Memory/Disk 使用率高</td></tr>
 *   <tr><td>CONFIG_ERROR</td><td>超时/连接池满/配置缺失</td></tr>
 *   <tr><td>EXTERNAL_DEPENDENCY</td><td>DB/Redis/外部 API 超时</td></tr>
 *   <tr><td>CODE_BUG</td><td>OOM/StackOverflow/NPE/死循环</td></tr>
 *   <tr><td>TRAFFIC_SPIKE</td><td>QPS 突增/并发过高</td></tr>
 *   <tr><td>NETWORK</td><td>超时/DNS/连接拒绝</td></tr>
 *   <tr><td>UNKNOWN</td><td>无法分类</td></tr>
 * </table>
 *
 * <p>配置项:
 * <pre>
 * minimax.monitor.rca.enabled=true
 * minimax.monitor.rca.model=MiniMax-Text-03
 * minimax.monitor.rca.service-url=http://localhost:8083
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRcaService {

    @Value("${minimax.monitor.rca.enabled:true}")
    private boolean enabled;

    @Value("${minimax.monitor.rca.model:MiniMax-Text-03}")
    private String rcaModel;

    @Value("${minimax.monitor.rca.service-url:http://localhost:8083}")
    private String modelServiceUrl;

    @Value("${minimax.monitor.rca-knowledge.enabled:true}")
    private boolean knowledgeEnabled;

    @Value("${minimax.monitor.rca-knowledge.inject-limit:5}")
    private int knowledgeInjectLimit;

    private final RestTemplate restTemplate;
    private final AlertRcaKnowledgeService rcaKnowledgeService;

    // ============== 公开 API ==============

    /**
     * 对告警事件进行根因分析。
     *
     * @param event       告警事件
     * @param recentEvents 最近的相关告警（可为空，用于上下文）
     * @return RCA 结果
     */
    public RcaResult analyze(AlertEvent event, List<AlertEvent> recentEvents) {
        if (!enabled) {
            return RcaResult.notAnalyzed(event.getId(), "RCA disabled");
        }

        long start = System.currentTimeMillis();
        try {
            // 1. 提取告警特征
            AlertProfile profile = buildProfile(event, recentEvents);

            // 2. 规则预分类（快速路径，不调 LLM）
            RootCauseCategory ruleCategory = ruleBasedCategory(event);
            if (ruleCategory != null && event.getDuration() != null && event.getDuration() < 60) {
                // 短时告警（<1min）且规则命中 → 直接返回，不调 LLM
                long elapsed = System.currentTimeMillis() - start;
                return RcaResult.of(event.getId(), ruleCategory, buildRuleBasedCause(event, ruleCategory),
                        buildRuleBasedActions(event, ruleCategory), elapsed, "rule-based (short-lived)");
            }

            // 3. LLM 深度推理
            return llmAnalyze(event, profile, recentEvents, start);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[RCA] alert {} failed: {}", event.getId(), e.getMessage(), e);
            return RcaResult.error(event.getId(), e.getMessage(), elapsed);
        }
    }

    /**
     * 简化版：仅分析单个事件。
     */
    /** V6.8.2 兼容别名 */
    public RcaResult analyzeRca(AlertEvent event) {
        return analyze(event);
    }

    public RcaResult analyze(AlertEvent event) {
        return analyze(event, List.of());
    }

    /**
     * 批量分析：同时分析多个告警事件。
     */
    public List<RcaResult> analyzeBatch(List<AlertEvent> events) {
        return events.stream().map(this::analyze).toList();
    }

    // ============== 特征构建 ==============

    /** 构建告警上下文特征 */
    private AlertProfile buildProfile(AlertEvent event, List<AlertEvent> recent) {
        AlertProfile p = new AlertProfile();
        p.event = event;
        p.metricName = event.getMetricName();
        p.severity = event.getSeverity();
        p.metricValue = event.getMetricValue();
        p.threshold = event.getThreshold();
        p.message = event.getMessage();
        p.duration = event.getDuration();
        p.ruleName = event.getRuleName();
        p.recentCount = recent.size();

        // 计算近期同类告警次数
        if (recent != null && !recent.isEmpty()) {
            p.recentSameCount = recent.stream()
                    .filter(e -> Objects.equals(e.getMetricName(), event.getMetricName()))
                    .count();
            p.recentSeverityCounts = recent.stream()
                    .collect(Collectors.groupingBy(AlertEvent::getSeverity, Collectors.counting()));
        }

        // 计算偏离度
        if (event.getMetricValue() != null && event.getThreshold() != null) {
            double threshold = event.getThreshold().doubleValue();
            if (threshold > 0) {
                p.deviationRatio = event.getMetricValue().doubleValue() / threshold;
            }
        }

        return p;
    }

    // ============== 规则预分类 ==============

    /** 基于规则的快速分类 */
    private RootCauseCategory ruleBasedCategory(AlertEvent event) {
        String metric = (event.getMetricName() != null ? event.getMetricName().toLowerCase() : "");
        String msg = (event.getMessage() != null ? event.getMessage().toLowerCase() : "");

        if (metric.contains("cpu") || metric.contains("memory") || metric.contains("heap")) {
            return RootCauseCategory.RESOURCE_BOTTLENECK;
        }
        if (metric.contains("disk") || metric.contains("storage")) {
            return RootCauseCategory.RESOURCE_BOTTLENECK;
        }
        if (metric.contains("timeout") || metric.contains("pool") || msg.contains("connection")) {
            return RootCauseCategory.CONFIG_ERROR;
        }
        if (metric.contains("db") || metric.contains("redis") || metric.contains("mysql")) {
            return RootCauseCategory.EXTERNAL_DEPENDENCY;
        }
        if (msg.contains("oom") || msg.contains("npe") || msg.contains("stackover")
                || msg.contains("outofmemory") || msg.contains("null")) {
            return RootCauseCategory.CODE_BUG;
        }
        if (metric.contains("qps") || metric.contains("request") || metric.contains("traffic")) {
            return RootCauseCategory.TRAFFIC_SPIKE;
        }
        if (metric.contains("network") || metric.contains("connect") || msg.contains("refused")) {
            return RootCauseCategory.NETWORK;
        }
        return null;
    }

    private String buildRuleBasedCause(AlertEvent event, RootCauseCategory cat) {
        double val = event.getMetricValue() != null ? event.getMetricValue().doubleValue() : 0;
        double thresh = event.getThreshold() != null ? event.getThreshold().doubleValue() : 1;

        return switch (cat) {
            case RESOURCE_BOTTLENECK -> String.format(
                    "%s 使用率 %.1f%% (阈值 %.1f%%)，超出 %.0f%%，可能存在资源泄漏或热点",
                    event.getMetricName(), val, thresh, (val / thresh - 1) * 100);
            case CONFIG_ERROR -> String.format(
                    "%s 触发阈值 %.2f，当前值 %.2f，可能连接池配置不当或资源耗尽",
                    event.getMetricName(), thresh, val);
            case EXTERNAL_DEPENDENCY -> String.format(
                    "外部依赖 %s 响应异常 (值=%.2f / 阈值=%.2f)，可能是下游服务抖动",
                    event.getMetricName(), val, thresh);
            case CODE_BUG -> String.format(
                    "代码异常: %s，当前 %.2f / 阈值 %.2f",
                    event.getMetricName(), val, thresh);
            case TRAFFIC_SPIKE -> String.format(
                    "流量突增: QPS %.0f (阈值 %.0f)，增长 %.0f%%",
                    val, thresh, (val / Math.max(thresh, 1) - 1) * 100);
            case NETWORK -> String.format(
                    "网络问题: %s，值=%.2f / 阈值=%.2f",
                    event.getMetricName(), val, thresh);
            default -> "根因待分析";
        };
    }

    private List<String> buildRuleBasedActions(AlertEvent event, RootCauseCategory cat) {
        return switch (cat) {
            case RESOURCE_BOTTLENECK -> List.of(
                    "1. 检查 Pod/容器的资源限制（CPU/Memory Limit）",
                    "2. dump 堆内存分析是否存在内存泄漏",
                    "3. jstat/jstack 查看 GC 和线程状态",
                    "4. 如持续高负载，考虑扩容"
            );
            case CONFIG_ERROR -> List.of(
                    "1. 检查连接池配置（HikariCP/druid）",
                    "2. 查看超时配置是否合理",
                    "3. 重启服务释放连接池",
                    "4. 查看是否有慢查询导致连接堆积"
            );
            case EXTERNAL_DEPENDENCY -> List.of(
                    "1. 检查下游服务（DB/Redis/外部 API）是否可用",
                    "2. 查看依赖服务日志",
                    "3. 如短暂抖动，等待自动恢复",
                    "4. 确认无影响后可 Acknowledge"
            );
            case CODE_BUG -> List.of(
                    "1. 立即保存 Full GC dump / heap dump",
                    "2. 查看异常堆栈定位问题代码",
                    "3. 评估是否需要紧急回滚",
                    "4. 如 OOM，考虑先扩容撑住"
            );
            case TRAFFIC_SPIKE -> List.of(
                    "1. 查看是否有活动/爬虫/攻击",
                    "2. 临时开启限流保护",
                    "3. 弹性扩容应对峰值",
                    "4. 分析流量来源"
            );
            case NETWORK -> List.of(
                    "1. 检查 DNS 解析是否正常",
                    "2. 查看网络延迟监控",
                    "3. 确认防火墙/安全组规则",
                    "4. 如外部依赖，联系供应商"
            );
            default -> List.of("1. 手动排查告警详情", "2. 如无法解决，升级人工处理");
        };
    }

    // ============== LLM 深度推理 ==============

    /** 通过 LLM 进行根因推理（含知识库上下文增强） */
    private RcaResult llmAnalyze(AlertEvent event, AlertProfile profile,
                                 List<AlertEvent> recentEvents, long startTime) {
        // 先查同类历史告警处理经验（知识库增强，Day 54）
        KnowledgeContext knowledge = buildKnowledgeContext(event);

        String context = buildLlmPrompt(event, profile, recentEvents);
        if (!knowledge.context().isBlank()) {
            context = knowledge.context() + "\n\n" + context;
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", rcaModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", buildSystemPrompt()),
                            Map.of("role", "user", "content", context)
                    ),
                    "max_tokens", 600,
                    "temperature", 0.2
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    modelServiceUrl + "/models/chat",
                    body, Map.class);

            String answer = extractContent(resp);
            long elapsed = System.currentTimeMillis() - startTime;

            if (answer == null || answer.isBlank()) {
                return RcaResult.notAnalyzed(event.getId(), "LLM returned empty");
            }

            return parseLlmResult(event.getId(), answer, elapsed, knowledge.entries());

        } catch (Exception e) {
            log.warn("[RCA] LLM call failed for alert {}: {}", event.getId(), e.getMessage());
            long elapsed = System.currentTimeMillis() - startTime;
            // LLM 失败时降级到规则
            RootCauseCategory cat = ruleBasedCategory(event);
            if (cat != null) {
                return RcaResult.of(event.getId(), cat,
                        buildRuleBasedCause(event, cat),
                        buildRuleBasedActions(event, cat),
                        elapsed, "rule-based fallback (LLM failed)");
            }
            return RcaResult.error(event.getId(), e.getMessage(), elapsed);
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一位 SRE（Site Reliability Engineer）专家，专门分析生产环境告警的根因。

                请分析告警上下文，给出结构化的根因分析报告，格式如下：

                ## 根因分类
                [RESOURCE_BOTTLENECK / CONFIG_ERROR / EXTERNAL_DEPENDENCY / CODE_BUG / TRAFFIC_SPIKE / NETWORK / UNKNOWN]

                ## 根因分析
                [3-5句话描述可能的根因，引用具体数据]

                ## 建议操作
                [4条按优先级排序的具体操作步骤，每条一行]

                注意：
                - 只输出上述三部分，不要额外解释
                - 分类必须是上述7种类别之一
                - 建议操作要具体可执行
                """;
    }

    /** 从知识库拉取同类历史告警处理经验，注入 prompt（Day 54） */
    private KnowledgeContext buildKnowledgeContext(AlertEvent event) {
        if (!knowledgeEnabled) return new KnowledgeContext("", List.of());
        try {
            List<AlertRcaKnowledgeService.KnowledgeEntry> history =
                    rcaKnowledgeService.findSimilar(event.getId(), 30, knowledgeInjectLimit);
            if (history == null || history.isEmpty()) {
                // 尝试按 metricName 直接匹配
                history = rcaKnowledgeService.queryKnowledge(event.getMetricName(), 30, knowledgeInjectLimit);
            }
            if (history == null || history.isEmpty()) return new KnowledgeContext("", List.of());

            StringBuilder sb = new StringBuilder();
            sb.append("## 同类历史告警处理经验（来自知识库）\n");
            for (int i = 0; i < history.size(); i++) {
                AlertRcaKnowledgeService.KnowledgeEntry e = history.get(i);
                sb.append(String.format(
                        "[历史%ds] %s | %s | %s | 持续 %s | 处理人: %s\n",
                        (i + 1),
                        e.severity(),
                        e.ruleName() != null ? e.ruleName() : e.metricName(),
                        e.status(),
                        e.duration() != null ? formatDuration(e.duration()) : "未知",
                        e.resolvedBy() != null ? e.resolvedBy() : "未知"
                ));
                if (e.notes() != null && !e.notes().isBlank()) {
                    sb.append("  → 处理方案: ").append(e.notes()).append("\n");
                }
            }
            log.info("[RCA] alert {} enriched with {} historical knowledge entries",
                    event.getId(), history.size());
            return new KnowledgeContext(sb.toString(), history);
        } catch (Exception ex) {
            log.warn("[RCA] Failed to fetch knowledge for alert {}: {}", event.getId(), ex.getMessage());
            return new KnowledgeContext("", List.of());
        }
    }

    /** 格式化时长（秒 → 友好字符串） */
    private String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return String.format("%.1fh", seconds / 3600.0);
    }

    /** 知识上下文：prompt 注入字符串 + 结构化条目 */
    private record KnowledgeContext(String context, List<AlertRcaKnowledgeService.KnowledgeEntry> entries) {}

    private String buildLlmPrompt(AlertEvent event, AlertProfile profile,
                                  List<AlertEvent> recent) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 告警信息\n");
        sb.append(String.format("- 规则名: %s\n", event.getRuleName()));
        sb.append(String.format("- 指标: %s\n", event.getMetricName()));
        sb.append(String.format("- 严重度: %s\n", event.getSeverity()));
        sb.append(String.format("- 当前值: %s\n", event.getMetricValue()));
        sb.append(String.format("- 阈值: %s\n", event.getThreshold()));
        sb.append(String.format("- 消息: %s\n", event.getMessage()));
        sb.append(String.format("- 触发时间: %s\n", event.getFiredAt()));
        sb.append(String.format("- 持续时间: %s 秒\n",
                event.getDuration() != null ? event.getDuration() : "未知"));
        sb.append(String.format("- 偏离度: %.2fx\n", profile.deviationRatio));
        sb.append(String.format("- 近期同类告警: %d 次\n", profile.recentSameCount));

        if (recent != null && !recent.isEmpty()) {
            sb.append("\n## 近期相关告警 (最近10条)\n");
            recent.stream().limit(10).forEach(e ->
                    sb.append(String.format("- [%s] %s | %s=%.2f | %s\n",
                            e.getSeverity(), e.getRuleName(), e.getMetricName(),
                            e.getMetricValue() != null ? e.getMetricValue().doubleValue() : 0,
                            e.getMessage()))
            );
        }

        sb.append("\n请按要求输出根因分析报告：");
        return sb.toString();
    }

    /** 解析 LLM 返回结果 */
    private RcaResult parseLlmResult(Long alertId, String answer, long elapsed,
                                     List<AlertRcaKnowledgeService.KnowledgeEntry> historicalKnowledge) {
        RootCauseCategory category = RootCauseCategory.UNKNOWN;
        String cause = "";
        List<String> actions = new ArrayList<>();

        // 解析分类
        for (RootCauseCategory cat : RootCauseCategory.values()) {
            if (answer.contains(cat.name())) {
                category = cat;
                break;
            }
        }

        // 解析根因（找 "## 根因分析" 后的内容）
        int causeStart = answer.indexOf("## 根因分析");
        int causeEnd = answer.indexOf("## 建议操作");
        if (causeStart >= 0 && causeEnd > causeStart) {
            cause = answer.substring(causeStart + "## 根因分析".length(), causeEnd).strip();
        } else {
            // Fallback: 取分类后面的内容
            int catEnd = answer.indexOf(category.name()) + category.name().length();
            cause = answer.substring(catEnd, Math.min(catEnd + 300, answer.length())).strip();
        }

        // 解析建议操作
        int actionStart = answer.indexOf("## 建议操作");
        if (actionStart >= 0) {
            String actionBlock = answer.substring(actionStart + "## 建议操作".length()).strip();
            // 按行分割，过滤编号
            for (String line : actionBlock.split("\n")) {
                String trimmed = line.replaceFirst("^\\d+[.、、]\\s*", "").strip();
                if (!trimmed.isEmpty() && trimmed.length() > 5) {
                    actions.add(trimmed);
                }
            }
        }

        if (actions.isEmpty()) {
            actions = buildRuleBasedActions(null, category);
        }

        return new RcaResult(alertId, category, cause, actions,
                elapsed, "llm", 0.7, answer,
                historicalKnowledge != null ? historicalKnowledge : List.of());
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> resp) {
        if (resp == null) return null;
        Object content = resp.get("content");
        if (content instanceof String) return (String) content;
        if (content instanceof Map) return (String) ((Map<String, Object>) content).get("text");
        return String.valueOf(content);
    }

    // ============== 数据类 ==============

    private static class AlertProfile {
        AlertEvent event;
        String metricName, severity, message, ruleName;
        java.math.BigDecimal metricValue, threshold;
        Long duration;
        double deviationRatio = 1.0;
        long recentCount = 0, recentSameCount = 0;
        Map<String, Long> recentSeverityCounts = Map.of();
    }

    @Getter
    public static class RcaResult {
        private final Long alertId;
        private final RootCauseCategory category;
        private final String cause;
        private final List<String> suggestedActions;
        private final long analysisMs;
        private final String method;       // "rule-based" / "llm" / "fallback"
        private final double confidence;   // 0.0 ~ 1.0
        private final String rawAnswer;    // LLM 原始回答
        private final String error;
        /** Day 54: 历史知识条目（来自同类告警处理经验） */
        private final List<AlertRcaKnowledgeService.KnowledgeEntry> historicalKnowledge;

        public RcaResult(Long alertId, RootCauseCategory category, String cause,
                        List<String> suggestedActions, long analysisMs, String method,
                        double confidence, String rawAnswer,
                        List<AlertRcaKnowledgeService.KnowledgeEntry> historicalKnowledge) {
            this.alertId = alertId;
            this.category = category;
            this.cause = cause != null ? cause : "";
            this.suggestedActions = suggestedActions != null ? List.copyOf(suggestedActions) : List.of();
            this.analysisMs = analysisMs;
            this.method = method;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.rawAnswer = rawAnswer;
            this.error = null;
            this.historicalKnowledge = historicalKnowledge != null ? List.copyOf(historicalKnowledge) : List.of();
        }

        /** V6.8.2 兼容别名 */
        public String getRootCause() { return cause; }
        /** V6.8.2 兼容别名 */
        public List<String> getSuggestions() { return suggestedActions; }

        private RcaResult(Long alertId, String error, long analysisMs) {
            this.alertId = alertId;
            this.category = RootCauseCategory.UNKNOWN;
            this.cause = "";
            this.suggestedActions = List.of();
            this.analysisMs = analysisMs;
            this.method = "error";
            this.confidence = 0.0;
            this.rawAnswer = null;
            this.error = error;
            this.historicalKnowledge = List.of();
        }

        private RcaResult(Long alertId, String message) {
            this.alertId = alertId;
            this.category = RootCauseCategory.UNKNOWN;
            this.cause = "";
            this.suggestedActions = List.of();
            this.analysisMs = 0;
            this.method = "skipped";
            this.confidence = 0.0;
            this.rawAnswer = null;
            this.error = message;
            this.historicalKnowledge = List.of();
        }

        public static RcaResult of(Long alertId, RootCauseCategory cat,
                                   String cause, List<String> actions,
                                   long ms, String method) {
            return new RcaResult(alertId, cat, cause, actions, ms, method, 0.85, null, List.of());
        }

        public static RcaResult notAnalyzed(Long alertId, String reason) {
            return new RcaResult(alertId, reason);
        }

        public static RcaResult error(Long alertId, String err, long ms) {
            return new RcaResult(alertId, err, ms);
        }

        public boolean isAnalyzed() { return error == null && category != RootCauseCategory.UNKNOWN; }
        public boolean isSuccess() { return error == null; }
    }

    public enum RootCauseCategory {
        RESOURCE_BOTTLENECK,  // 资源瓶颈
        CONFIG_ERROR,         // 配置错误
        EXTERNAL_DEPENDENCY,  // 外部依赖
        CODE_BUG,             // 代码缺陷
        TRAFFIC_SPIKE,        // 流量突增
        NETWORK,              // 网络问题
        UNKNOWN               // 未知
    }
}
