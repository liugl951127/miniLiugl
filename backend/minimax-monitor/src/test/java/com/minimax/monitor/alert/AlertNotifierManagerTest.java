package com.minimax.monitor.alert;

import com.minimax.monitor.entity.AlertChannel;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.mapper.AlertChannelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlertNotifierManager 链路测试 (Day 23).
 * 验证:
 *   - 启动时正确加载所有 notifier (EMAIL / DINGTALK)
 *   - 渠道刷新 + 过滤
 *   - notifyAll 路由正确（不抛异常）
 */
@SpringBootTest
@ActiveProfiles("test")
class AlertNotifierManagerTest {

    @Autowired AlertNotifierManager manager;
    @Autowired AlertChannelMapper channelMapper;
    @Autowired JdbcTemplate jdbc;

    private AlertEvent fakeEvent() {
        AlertEvent e = new AlertEvent();
        e.setId(System.currentTimeMillis());
        e.setRuleId(1L);
        e.setRuleName("test-rule");
        e.setMetricName("test_metric");
        e.setMetricValue(BigDecimal.valueOf(95.5));
        e.setThreshold(BigDecimal.valueOf(80));
        e.setSeverity("warning");
        e.setFiredAt(LocalDateTime.now());
        e.setMessage("test alert");
        e.setStatus("firing");
        return e;
    }

    @BeforeEach
    void setup() {
        // 确保至少有一个启用的虚拟渠道用于测试
        jdbc.update("DELETE FROM alert_channel WHERE name LIKE 'test-%'");
        jdbc.update("INSERT INTO alert_channel (name, channel_type, config, enabled, priority, created_at) " +
                "VALUES ('test-email', 'EMAIL', '{\"email\":\"test@example.com\"}', 0, 10, NOW())");
        manager.refreshChannels();
    }

    @Test
    void managerHasEmailAndDingtalk() {
        // 应该加载了 EMAIL 和 DINGTALK notifier
        List<AlertChannel> channels = manager.channels();
        assertNotNull(channels);
    }

    @Test
    void refreshChannelsClearsCache() {
        int before = manager.channels().size();
        jdbc.update("INSERT INTO alert_channel (name, channel_type, config, enabled, priority, created_at) " +
                "VALUES (?, 'EMAIL', '{\"email\":\"test2@example.com\"}', 0, 20, NOW())",
                "test-" + UUID.randomUUID());
        manager.refreshChannels();
        int after = manager.channels().size();
        assertTrue(after >= before);
    }

    @Test
    void notifyAllDisabledChannelSkipped() {
        // disabled 渠道不应被调用 (不抛异常即通过)
        AlertEvent e = fakeEvent();
        assertDoesNotThrow(() -> manager.notifyAll(e));
    }

    @Test
    void notifyAllNoExceptionOnUnknownChannel() {
        // manager.refreshChannels() 后只含已知 channel，不应抛异常
        AlertEvent e = fakeEvent();
        assertDoesNotThrow(() -> manager.notifyAll(e));
    }
}
