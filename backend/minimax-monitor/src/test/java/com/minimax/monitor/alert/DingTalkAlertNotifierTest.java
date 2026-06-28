package com.minimax.monitor.alert;

import com.minimax.monitor.entity.AlertEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DingTalkAlertNotifier 单元测试 (Day 23).
 * 验证:
 *   - channelType 返回 "DINGTALK"
 *   - 空配置/null 返回 false，不抛异常
 *   - 正常配置不走网络 (使用 invalid URL 测试降级)
 */
class DingTalkAlertNotifierTest {

    private AlertEvent fakeEvent() {
        AlertEvent e = new AlertEvent();
        e.setId(1L);
        e.setRuleId(1L);
        e.setRuleName("test-dingtalk");
        e.setMetricName("memory_usage");
        e.setMetricValue(BigDecimal.valueOf(88.0));
        e.setThreshold(BigDecimal.valueOf(80));
        e.setSeverity("warning");
        e.setFiredAt(LocalDateTime.now());
        e.setMessage("Memory usage high");
        e.setResolved(0);
        return e;
    }

    private DingTalkAlertNotifier notifier() {
        DingTalkAlertNotifier n = new DingTalkAlertNotifier();
        // 不设置 httpClient，测试无网络场景
        return n;
    }

    @Test
    void channelTypeIsDingtalk() {
        assertEquals("DINGTALK", notifier().channelType());
    }

    @Test
    void nullConfigReturnsFalse() {
        assertFalse(notifier().send(fakeEvent(), null));
    }

    @Test
    void emptyConfigReturnsFalse() {
        assertFalse(notifier().send(fakeEvent(), ""));
    }

    @Test
    void missingWebhookReturnsFalse() {
        assertFalse(notifier().send(fakeEvent(), "{\"secret\": \"SECxxx\"}"));
    }

    @Test
    void emptyWebhookReturnsFalse() {
        assertFalse(notifier().send(fakeEvent(), "{\"webhook\": \"\"}"));
    }

    @Test
    void invalidWebhookUrlDoesNotThrow() {
        // 不抛异常，返回 false（因为 httpClient 未注入）
        AlertEvent e = fakeEvent();
        DingTalkAlertNotifier n = notifier();
        boolean result = n.send(e, "{\"webhook\": \"http://localhost:99999/invalid\"}");
        assertFalse(result);
    }

    @Test
    void allSeveritiesHandledGracefully() {
        DingTalkAlertNotifier n = notifier();
        String[] severities = { "critical", "warning", "info", "unknown" };
        for (String sev : severities) {
            AlertEvent e = fakeEvent();
            e.setSeverity(sev);
            // 不抛异常即可
            assertDoesNotThrow(() -> n.send(e, "{\"webhook\": \"http://localhost:99999\"}"));
        }
    }
}
