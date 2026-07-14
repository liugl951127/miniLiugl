package com.minimax.monitor.service;

import com.minimax.monitor.entity.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 告警通知模板解析器 (Day 28).
 *
 * 支持变量替换:
 *   ${ruleName}   - 规则名称
 *   ${severity}   - 严重度 (critical/warning/info)
 *   ${metricName} - 指标名称
 *   ${metricValue} - 当前指标值
 *   ${threshold}  - 阈值
 *   ${message}    - 告警消息
 *   ${firedAt}    - 触发时间 (yyyy-MM-dd HH:mm:ss)
 *   ${service}    - 关联服务
 *
 * 用法:
 *   String text = resolver.resolve(event, channel.getTemplate());
 *   // channel.template == null → 返回默认消息
 */
@Slf4j
@Component
public class AlertTemplateResolver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    /** 默认模板 */
    private static final String DEFAULT_TEMPLATE = """
            【${severity}】告警: ${ruleName}
            指标: ${metricName}
            当前值: ${metricValue}  |  阈值: ${threshold}
            触发时间: ${firedAt}
            消息: ${message}""";

    /**
     * 将模板字符串中的 ${变量} 替换为实际值。
     *
     * @param event    告警事件
     * @param template 模板字符串 (可 null → 用默认模板)
     * @return 替换后的文本
     */
    public String resolve(AlertEvent event, String template) {
        String tpl = (template != null && !template.isBlank()) ? template : DEFAULT_TEMPLATE;

        Matcher m = VAR_PATTERN.matcher(tpl);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String var = m.group(1);
            String replacement = substitute(var, event);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String substitute(String var, AlertEvent e) {
        return switch (var) {
            case "ruleName"    -> e.getRuleName() != null ? e.getRuleName() : "";
            case "severity"    -> e.getSeverity() != null ? e.getSeverity().toUpperCase() : "";
            case "metricName" -> e.getMetricName() != null ? e.getMetricName() : "";
            case "metricValue" -> e.getMetricValue() != null ? e.getMetricValue().toString() : "";
            case "threshold"   -> e.getThreshold() != null ? e.getThreshold().toString() : "";
            case "message"     -> e.getMessage() != null ? e.getMessage() : "";
            case "firedAt"     -> e.getFiredAt() != null ? e.getFiredAt().format(FMT) : "";
            case "service"     -> "";
            default            -> "${" + var + "}"; // 未识别变量原样保留
        };
    }

    /** 返回默认模板 */
    public String defaultTemplate() {
        return DEFAULT_TEMPLATE;
    }
}
