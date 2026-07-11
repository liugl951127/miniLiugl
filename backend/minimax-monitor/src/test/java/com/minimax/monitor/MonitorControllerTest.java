package com.minimax.monitor;

import com.minimax.common.result.Result;
import com.minimax.monitor.controller.MonitorController;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.entity.AlertRule;
import com.minimax.monitor.service.SnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MonitorController 链路测试 (Day 23).
 * 覆盖所有 /monitor/* 端点的基本响应。
 */
@SpringBootTest
@ActiveProfiles("test")
class MonitorControllerTest {

    @Autowired MonitorController controller;
    @Autowired SnapshotService snapshotService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void healthReturnsAllSections() {
        Result<Map<String, Object>> r = controller.deepHealth();
        assertNotNull(r);
        assertNotNull(r.getData());
        Map<String, Object> data = r.getData();
        assertTrue(data.containsKey("database"));
        assertTrue(data.containsKey("jvm"));
        assertTrue(data.containsKey("disk"));
        assertTrue(data.containsKey("overall"));
    }

    @Test
    void healthDatabaseUp() {
        Result<Map<String, Object>> r = controller.db();
        assertNotNull(r.getData());
        assertEquals("UP", r.getData().get("status"));
    }

    @Test
    void healthJvmHasHeap() {
        Result<Map<String, Object>> r = controller.jvm();
        assertNotNull(r.getData());
        assertNotNull(r.getData().get("heap"));
    }

    @Test
    void healthDiskHasUsage() {
        Result<Map<String, Object>> r = controller.disk();
        assertNotNull(r.getData());
        assertTrue(r.getData().containsKey("usagePercent"));
    }

    @Test
    void metricsReturnsCounters() {
        Result<Map<String, Object>> r = controller.metrics();
        assertNotNull(r.getData());
        Map<String, Object> data = r.getData();
        assertTrue(data.containsKey("chat_messages_total"));
        assertTrue(data.containsKey("tool_calls_total"));
        assertTrue(data.containsKey("rag_queries_total"));
    }

    @Test
    void snapshotReturnsList() {
        // 先写入一条测试快照
        snapshotService.saveSnap("test-svc", "test_metric", 42.5, "{}");
        var r = controller.snapshot("test_metric", "test-svc", 60, 10);
        assertNotNull(r.getData());
    }

    @Test
    void alertsReturnsList() {
        var r = controller.alerts(20);
        assertNotNull(r.getData());
        assertTrue(r.getData() instanceof List);
    }

    @Test
    void alertsFiringReturnsList() {
        var r = controller.firing(20);
        assertNotNull(r.getData());
    }

    @Test
    void alertsRulesReturnsList() {
        var r = controller.rules();
        assertNotNull(r.getData());
        assertTrue(r.getData() instanceof List);
    }

    @Test
    void alertsSummaryHasKeys() {
        var r = controller.alertSummary();
        assertNotNull(r.getData());
        Map<String, Object> data = r.getData();
        assertTrue(data.containsKey("totalRules") || data.containsKey("firingCount"));
    }

    @Test
    void monitorInfoReturnsVersion() {
        var r = controller.info();
        assertNotNull(r.getData());
        java.util.Map<String, Object> data = r.getData();
        assertTrue(data.containsKey("version") || data.containsKey("name"));
    }

    @Test
    void alertRuleCrudCycle() {
        // create
        com.minimax.monitor.entity.AlertRule rule = new com.minimax.monitor.entity.AlertRule();
        rule.setName("e2e-test-rule");
        rule.setMetricName("test_metric");
        rule.setOperator(">");
        rule.setThreshold(new BigDecimal("50"));
        rule.setCooldownMinutes(5);
        rule.setSeverity("warning");
        var createR = controller.createRule(rule);
        assertNotNull(createR.getData());
        Long id = createR.getData().getId();

        // list all
        var listR = controller.allRules();
        assertTrue(listR.getData().stream().anyMatch(r -> "e2e-test-rule".equals(r.getName())));

        // update
        createR.getData().setThreshold(new BigDecimal("60"));
        var updateR = controller.updateRule(id, createR.getData());
        assertEquals(0, new BigDecimal("60").compareTo(updateR.getData().getThreshold()));

        // delete
        var delR = controller.deleteRule(id);
        assertEquals(200, delR.getCode());
    }
}
