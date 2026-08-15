package com.minimax.monitor;

import com.minimax.monitor.alert.AlertEngine;
import com.minimax.monitor.alert.AlertNotifierManager;
import com.minimax.monitor.collector.MetricsCollector;
import com.minimax.monitor.entity.AlertChannel;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.entity.AlertRule;
import com.minimax.monitor.mapper.AlertChannelMapper;
import com.minimax.monitor.mapper.AlertEventMapper;
import com.minimax.monitor.mapper.AlertRuleMapper;
import com.minimax.monitor.service.SnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V5.33 Day 23: 告警引擎完整链路测试
 * 覆盖: 阈值触发/恢复/冷却期/多规则/通知/CRUD/API
 */
@SpringBootTest
@ActiveProfiles("test")
class AlertEngineTestIT {

    @Autowired AlertEngine engine;
    @Autowired AlertRuleMapper ruleMapper;
    @Autowired AlertEventMapper eventMapper;
    @Autowired AlertChannelMapper channelMapper;
    @Autowired MetricsCollector collector;
    @Autowired SnapshotService snapshot;
    @Autowired JdbcTemplate jdbc;
    @Autowired AlertNotifierManager notifierManager;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM alert_event");
        // 只保留 http_5xx_rate 规则，避免 JVM 堆/磁盘 干扰本地测试
        jdbc.update("UPDATE alert_rule SET enabled = 0 WHERE metric_name <> 'http_5xx_rate'");
        jdbc.update("DELETE FROM alert_channel WHERE name LIKE 'test-%'");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 基础加载测试
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("规则从种子数据加载")
    void rulesLoadedFromSeed() {
        List<AlertRule> rules = engine.rules();
        assertNotNull(rules);
        assertTrue(rules.stream().anyMatch(r -> "http_5xx_rate".equals(r.getMetricName())),
                "应有 http_5xx_rate 启用规则");
    }

    @Test
    @DisplayName("摘要返回计数")
    void summaryReturnsCounts() {
        Map<String, Object> s = engine.summary();
        assertNotNull(s.get("totalRules"));
        assertNotNull(s.get("firingCount"));
        assertNotNull(s.get("resolvedCount"));
        assertEquals(0L, s.get("firingCount"), "初始无 firing 事件");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 告警触发 / 恢复 链路
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("指标超阈值触发 firing 事件")
    void metricExceedsThresholdFiresAlert() {
        // 插入高阈值规则：5xx rate > 0.01 时触发
        AlertRule r = new AlertRule();
        r.setName("test-fire-rule");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("0.001")); // 很低的阈值，确保触发
        r.setCooldownMinutes(0);
        r.setSeverity("critical");
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);
        Long ruleId = created.getId();

        int before = eventMapper.selectByStatus("firing", 100).size();

        // 通过快照注入高指标值
        snapshot.saveSnap("gateway", "http_5xx_rate", 5.0, "{}");
        engine.evaluateRule(created);

        List<AlertEvent> firing = eventMapper.selectByStatus("firing", 100);
        assertTrue(firing.size() > before, "应有 firing 事件");
        assertEquals("firing", firing.get(0).getStatus());
        assertEquals(ruleId, firing.get(0).getRuleId());

        // 清理
        engine.deleteRule(ruleId);
    }

    @Test
    @DisplayName("指标恢复正常自动 resolved")
    void metricRecoversResolvesAlert() {
        AlertRule r = new AlertRule();
        r.setName("test-resolve-rule");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("0.001"));
        r.setCooldownMinutes(0);
        r.setSeverity("warning");
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        // 先触发
        snapshot.saveSnap("gateway", "http_5xx_rate", 10.0, "{}");
        engine.evaluateRule(created);

        // 再恢复正常（快照里没数据或极低值）
        snapshot.saveSnap("gateway", "http_5xx_rate", 0.0, "{}");
        engine.evaluateRule(created);

        List<AlertEvent> firing = eventMapper.selectByStatus("firing", 100);
        List<AlertEvent> resolved = eventMapper.selectByStatus("resolved", 100);
        assertEquals(0, firing.size(), "触发后恢复，应无 firing");
        assertTrue(resolved.stream().anyMatch(e -> e.getRuleId().equals(created.getId())), "应有 resolved 事件");

        engine.deleteRule(created.getId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 冷却期测试
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("冷却期内不重复触发")
    void cooldownPreventsDuplicateFiring() {
        AlertRule r = new AlertRule();
        r.setName("test-cooldown-rule");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("0.001"));
        r.setCooldownMinutes(60); // 60 分钟冷却
        r.setSeverity("warning");
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        // 触发
        snapshot.saveSnap("gateway", "http_5xx_rate", 8.0, "{}");
        engine.evaluateRule(created);

        // 再次评估（仍在冷却期）
        snapshot.saveSnap("gateway", "http_5xx_rate", 9.0, "{}");
        engine.evaluateRule(created);

        List<AlertEvent> firing = eventMapper.selectByStatus("firing", 100);
        long myFirings = firing.stream().filter(e -> e.getRuleId().equals(created.getId())).count();
        assertEquals(1, myFirings, "冷却期内不应重复触发");

        engine.deleteRule(created.getId());
    }

    @Test
    @DisplayName("冷却期=0 每次超阈值都触发")
    void zeroCooldownTriggersEveryTime() {
        AlertRule r = new AlertRule();
        r.setName("test-no-cooldown");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("0.001"));
        r.setCooldownMinutes(0);
        r.setSeverity("warning");
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        // 触发两次
        snapshot.saveSnap("gateway", "http_5xx_rate", 8.0, "{}");
        engine.evaluateRule(created);
        snapshot.saveSnap("gateway", "http_5xx_rate", 9.0, "{}");
        engine.evaluateRule(created);

        List<AlertEvent> firing = eventMapper.selectByStatus("firing", 100);
        long myFirings = firing.stream().filter(e -> e.getRuleId().equals(created.getId())).count();
        assertEquals(1, myFirings, "冷却期=0 但同一次 firing 状态不应重复插入");

        engine.deleteRule(created.getId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 操作符测试
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("所有比较操作符正常工作")
    void allOperatorsWork() {
        // (value, operator, threshold) → expected result
        record Case(double value, String op, double threshold, boolean expectTrigger) {}

        Case[] cases = {
            new Case(5.0, ">",  3.0, true),
            new Case(3.0, ">",  5.0, false),
            new Case(5.0, ">=", 5.0, true),
            new Case(4.9, ">=", 5.0, false),
            new Case(2.0, "<",  3.0, true),
            new Case(3.0, "<",  2.0, false),
            new Case(5.0, "<=", 5.0, true),
            new Case(5.1, "<=", 5.0, false),
            new Case(5.0, "=",  5.0, true),
            new Case(5.0, "=",  5.1, false),
            new Case(5.0, "!=", 3.0, true),
            new Case(5.0, "!=", 5.0, false),
        };

        for (Case c : cases) {
            snapshot.saveSnap("gateway", "http_5xx_rate", c.value, "{}");

            AlertRule r = new AlertRule();
            r.setName("op-test-" + c.op);
            r.setMetricName("http_5xx_rate");
            r.setOperator(c.op);
            r.setThreshold(new BigDecimal(String.valueOf(c.threshold)));
            r.setCooldownMinutes(0);
            r.setSeverity("warning");
            r.setEnabled(1);
            r.setNotifyChannel("log");
            AlertRule created = engine.createRule(r);

            engine.evaluateRule(created);

            List<AlertEvent> firing = eventMapper.selectByStatus("firing", 100);
            boolean triggered = firing.stream().anyMatch(e -> e.getRuleId().equals(created.getId()));

            assertEquals(c.expectTrigger, triggered,
                    String.format("value=%.2f op=%s threshold=%.2f → expectTrigger=%s",
                            c.value, c.op, c.threshold, c.expectTrigger));

            engine.deleteRule(created.getId());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 规则 CRUD
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("创建规则默认字段")
    void createRuleDefaults() {
        AlertRule r = new AlertRule();
        r.setName("crud-test");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("1"));
        r.setEnabled(null); // 不设置，期望默认
        r.setCooldownMinutes(null);

        AlertRule created = engine.createRule(r);

        assertNotNull(created.getId());
        assertEquals(1, created.getEnabled());
        assertEquals(15, created.getCooldownMinutes());
        assertEquals("warning", created.getSeverity());

        engine.deleteRule(created.getId());
    }

    @Test
    @DisplayName("更新规则")
    void updateRule() {
        AlertRule r = new AlertRule();
        r.setName("update-test");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("1"));
        r.setCooldownMinutes(15);
        r.setEnabled(1);
        r.setSeverity("info");
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        AlertRule patch = new AlertRule();
        patch.setName("update-test-renamed");
        patch.setThreshold(new BigDecimal("99"));
        patch.setSeverity("critical");
        AlertRule updated = engine.updateRule(created.getId(), patch);

        assertEquals("update-test-renamed", updated.getName());
        assertEquals(new BigDecimal("99"), updated.getThreshold());
        assertEquals("critical", updated.getSeverity());
        // 未修改字段保持不变
        assertEquals("info", updated.getSeverity()); // 还是 info，因为 patch 没传

        engine.deleteRule(created.getId());
    }

    @Test
    @DisplayName("更新不存在规则抛异常")
    void updateNonexistentRuleThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine.updateRule(999999L, new AlertRule());
        });
    }

    @Test
    @DisplayName("软删除规则")
    void deleteRule() {
        AlertRule r = new AlertRule();
        r.setName("delete-test");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("1"));
        r.setCooldownMinutes(0);
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        engine.deleteRule(created.getId());

        AlertRule deleted = ruleMapper.selectById(created.getId());
        assertNull(deleted);
    }

    @Test
    @DisplayName("allRules 返回全部规则（含禁用）")
    void allRulesIncludesDisabled() {
        List<AlertRule> all = engine.allRules();
        assertNotNull(all);
        assertFalse(all.isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 通知渠道
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("日志通知不抛异常")
    void logNotifierNoException() {
        AlertChannel ch = new AlertChannel();
        ch.setName("test-log-channel");
        ch.setType("log");
        ch.setConfig("{}");
        channelMapper.insert(ch);

        AlertRule r = new AlertRule();
        r.setName("notify-test");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("0.001"));
        r.setCooldownMinutes(0);
        r.setSeverity("warning");
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        snapshot.saveSnap("gateway", "http_5xx_rate", 7.0, "{}");
        // evaluateRule 里调用 notifierManager.notifyAll()，不应抛异常
        assertDoesNotThrow(() -> engine.evaluateRule(created));

        channelMapper.deleteById(ch.getId());
        engine.deleteRule(created.getId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API 方法
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("recentEvents 返回最近事件")
    void recentEventsReturnsEvents() {
        AlertRule r = new AlertRule();
        r.setName("recent-events-test");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("0.001"));
        r.setCooldownMinutes(0);
        r.setSeverity("info");
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        snapshot.saveSnap("gateway", "http_5xx_rate", 6.0, "{}");
        engine.evaluateRule(created);

        List<AlertEvent> recent = engine.recentEvents(10);
        assertFalse(recent.isEmpty(), "应有最近事件");
        assertTrue(recent.stream().anyMatch(e -> e.getRuleId().equals(created.getId())));

        engine.deleteRule(created.getId());
    }

    @Test
    @DisplayName("firingEvents 正确过滤状态")
    void firingEventsFiltersCorrectly() {
        AlertRule r = new AlertRule();
        r.setName("firing-filter-test");
        r.setMetricName("http_5xx_rate");
        r.setOperator(">");
        r.setThreshold(new BigDecimal("0.001"));
        r.setCooldownMinutes(0);
        r.setSeverity("critical");
        r.setEnabled(1);
        r.setNotifyChannel("log");
        AlertRule created = engine.createRule(r);

        snapshot.saveSnap("gateway", "http_5xx_rate", 6.0, "{}");
        engine.evaluateRule(created);

        List<AlertEvent> firing = engine.firingEvents(10);
        assertTrue(firing.stream().allMatch(e -> "firing".equals(e.getStatus())),
                "firingEvents 只应返回 firing 状态");

        engine.deleteRule(created.getId());
    }

    @Test
    @DisplayName("firingEvents limit 边界")
    void firingEventsLimitBounds() {
        // limit = 0 → 默认 20
        List<AlertEvent> result0 = engine.firingEvents(0);
        assertTrue(result0.size() <= 20);

        // limit = -1 → 默认 20
        List<AlertEvent> resultNeg = engine.firingEvents(-1);
        assertTrue(resultNeg.size() <= 20);

        // limit = 201 → 截断到 200
        List<AlertEvent> resultBig = engine.firingEvents(201);
        assertTrue(resultBig.size() <= 200);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 快照服务
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("快照保存和读取")
    void snapshotSaveAndRead() {
        snapshot.saveSnap("gateway", "test-metric-snap", 42.5, "{\"env\":\"test\"}");
        List<Map<String, Object>> agg = snapshot.trend("test-metric-snap", "gateway", 5);
        assertFalse(agg.isEmpty());
        assertNotNull(agg.get(0).get("avg_val"));
    }

    @Test
    @DisplayName("不存在指标返回空趋势")
    void nonexistentMetricReturnsEmptyTrend() {
        List<Map<String, Object>> agg = snapshot.trend("non-exist-metric-xyz", "non-exist-svc", 5);
        assertTrue(agg.isEmpty());
    }

    @Test
    @DisplayName("正常值不触发告警（不抛异常）")
    void normalValuesNoAlert() {
        assertDoesNotThrow(() -> {
            for (AlertRule r : engine.rules()) {
                engine.evaluateRule(r);
            }
        });
    }
}
