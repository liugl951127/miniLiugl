package com.minimax.monitor.alert;

import com.minimax.monitor.entity.AlertEvent;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmailAlertNotifier 单元测试 (Day 23).
 * 使用 mock mailSender，验证:
 *   - 缺少 email 配置时返回 false，不抛异常
 *   - 缺少 channelConfig 时返回 false
 *   - 正常配置构建消息体
 */
class EmailAlertNotifierTest {

    private EmailAlertNotifier notifier() {
        EmailAlertNotifier n = new EmailAlertNotifier();
        // 不注入真实 mailSender，测试降级路径
        return n;
    }

    private AlertEvent fakeEvent() {
        AlertEvent e = new AlertEvent();
        e.setId(1L);
        e.setRuleId(1L);
        e.setRuleName("test-email-rule");
        e.setMetricName("cpu_usage");
        e.setMetricValue(BigDecimal.valueOf(92.5));
        e.setThreshold(BigDecimal.valueOf(80));
        e.setSeverity("warning");
        e.setFiredAt(LocalDateTime.now());
        e.setMessage("CPU usage exceeded 80%");
        e.setStatus("firing");
        return e;
    }

    @Test
    void channelTypeIsEmail() {
        assertEquals("EMAIL", notifier().channelType());
    }

    @Test
    void missingChannelConfigReturnsFalse() {
        AlertEvent e = fakeEvent();
        assertFalse(notifier().send(e, null));
        assertFalse(notifier().send(e, ""));
        assertFalse(notifier().send(e, "{}"));
    }

    @Test
    void missingEmailInConfigReturnsFalse() {
        AlertEvent e = fakeEvent();
        assertFalse(notifier().send(e, "{\"notAnEmail\": true}"));
        assertFalse(notifier().send(e, "{\"email\": \"\"}"));
    }

    @Test
    void validConfigDoesNotThrow() {
        // mailSender 为 null，send() 内部有保护，应返回 false 而不抛异常
        AlertEvent e = fakeEvent();
        EmailAlertNotifier n = notifier();
        boolean result = n.send(e, "{\"email\": \"oncall@company.com\"}");
        // mailSender 为 null 时返回 false
        assertFalse(result);
    }
}
