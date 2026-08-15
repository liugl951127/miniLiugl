package com.minimax.monitor.alert;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.json.JSONObject;
import com.minimax.monitor.entity.AlertEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 邮件告警通知器 (V5.33 Day 18).
 *
 * 配置示例 (alert_channel.config JSON):
 *   {"email": "oncall@company.com"}
 */
@Slf4j
@Component
public class EmailAlertNotifier implements AlertNotifier {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String defaultFrom;

    @Value("${minimax.alert.email.from:alert@minimax.com}")
    private String from;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() {
        if (from == null || from.isBlank()) {
            from = defaultFrom;
        }
    }

    @Override
    public boolean send(AlertEvent event, String channelConfig, String resolvedText) {
        if (mailSender == null) {
            log.debug("mailSender not available, skip email");
            return false;
        }
        try {
            JSONObject cfg = new JSONObject(channelConfig);
            String toEmail = cfg.getStr("email");
            if (toEmail == null || toEmail.isBlank()) {
                log.warn("email not configured in channel: {}", channelConfig);
                return false;
            }

            String subject = String.format("[%s] 告警: %s",
                    event.getSeverity() != null ? event.getSeverity().toUpperCase() : "",
                    event.getRuleName() != null ? event.getRuleName() : "");
            String body = resolvedText != null ? resolvedText : buildBody(event);

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);

            mailSender.send(msg);
            log.info("email alert sent (templated) to {} for rule {}", toEmail, event.getRuleName());
            return true;
        } catch (Exception e) {
            log.warn("email alert send failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean send(AlertEvent event, String channelConfig) {
        if (mailSender == null) {
            log.debug("mailSender not available, skip email");
            return false;
        }
        try {
            JSONObject cfg = new JSONObject(channelConfig);
            String toEmail = cfg.getStr("email");
            if (toEmail == null || toEmail.isBlank()) {
                log.warn("email not configured in channel: {}", channelConfig);
                return false;
            }

            String subject = String.format("[%s] 告警: %s", event.getSeverity().toUpperCase(), event.getRuleName());
            String body = buildBody(event);

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);

            mailSender.send(msg);
            log.info("email alert sent to {} for rule {}", toEmail, event.getRuleName());
            return true;
        } catch (Exception e) {
            log.warn("email alert send failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String channelType() {
        return "EMAIL";
    }

    private String buildBody(AlertEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("告警通知\n");
        sb.append("========\n");
        sb.append("规则: ").append(e.getRuleName()).append("\n");
        sb.append("级别: ").append(e.getSeverity()).append("\n");
        sb.append("指标: ").append(e.getMetricName()).append("\n");
        sb.append("当前值: ").append(e.getMetricValue()).append("\n");
        sb.append("阈值: ").append(e.getThreshold()).append("\n");
        sb.append("触发时间: ").append(e.getFiredAt().format(FMT)).append("\n");
        if (e.getMessage() != null) {
            sb.append("消息: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n请及时处理。\n");
        return sb.toString();
    }
}
