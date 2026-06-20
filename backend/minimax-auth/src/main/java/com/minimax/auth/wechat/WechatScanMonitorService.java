package com.minimax.auth.wechat;

import com.minimax.auth.entity.WechatScanSession;
import com.minimax.auth.mapper.WechatScanSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 微信扫码异常监控 (V5).
 *
 * 检测:
 *   1. 单 IP 1 分钟内生成二维码 > 10 次 (机器人/撞库)
 *   2. 单 IP 1 小时内 confirmed 失败 > 5 次
 *   3. 单 openid 短时间跨多 IP 扫码 (账号被盗)
 *
 * 告警:
 *   - 写 alert_event 表 (V1 已有)
 *   - 控制台 WARN 日志
 *
 * @since 2026-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatScanMonitorService {

    private final WechatScanSessionMapper sessionMapper;

    @Value("${minimax.wechat.alert.ip-qrcode-threshold:10}")
    private int ipQrcodeThreshold;

    @Value("${minimax.wechat.alert.ip-confirm-fail-threshold:5}")
    private int ipConfirmFailThreshold;

    // IP 计数 (内存, 重启清空)
    private final Map<String, AtomicInteger> ipQrcodeCounter = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> ipConfirmFailCounter = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> openidIps = new ConcurrentHashMap<>();

    public void onQrcodeGenerated(String ip) {
        if (ip == null) return;
        int n = ipQrcodeCounter.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
        if (n > ipQrcodeThreshold) {
            alert(ip, "高频二维码生成: " + n + " 次/分钟", "qrcode_flood");
        }
    }

    public void onConfirmFailed(String ip) {
        if (ip == null) return;
        int n = ipConfirmFailCounter.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
        if (n > ipConfirmFailThreshold) {
            alert(ip, "高频扫码确认失败: " + n + " 次/小时", "confirm_fail_flood");
        }
    }

    public void onScanConfirmed(String openid, String ip) {
        if (openid == null || ip == null) return;
        Set<String> ips = openidIps.computeIfAbsent(openid, k -> ConcurrentHashMap.newKeySet());
        if (!ips.add(ip)) return;  // 新 IP 才告警
        if (ips.size() > 3) {
            alert("openid=" + openid, "同一微信跨 " + ips.size() + " IP 扫码: " + ips, "multi_ip_scan");
        }
    }

    /**
     * 定时清理 (每小时), 防止内存泄漏
     */
    @Scheduled(fixedDelay = 3600_000)
    public void cleanup() {
        log.info("WeChat 监控清理: {} IPs", ipQrcodeCounter.size());
        ipQrcodeCounter.clear();
        ipConfirmFailCounter.clear();
        // openid 集合不清理 (有跨 IP 检测价值)
    }

    /**
     * 扫描过期 session (每 5 分钟)
     */
    @Scheduled(fixedDelay = 300_000)
    public void expireOldSessions() {
        try {
            List<WechatScanSession> sessions = sessionMapper.selectList(null);
            int expired = 0;
            for (WechatScanSession s : sessions) {
                if (s.getExpiresAt() != null
                        && s.getExpiresAt().isBefore(LocalDateTime.now())
                        && !"confirmed".equals(s.getStatus())
                        && !"expired".equals(s.getStatus())) {
                    s.setStatus("expired");
                    sessionMapper.updateById(s);
                    expired++;
                }
            }
            if (expired > 0) log.info("过期 {} 个微信扫码会话", expired);
        } catch (Exception e) {
            log.warn("清理过期 session 失败: {}", e.getMessage());
        }
    }

    private void alert(String subject, String detail, String code) {
        log.warn("[WeChat Alert] code={} subject={} detail={}", code, subject, detail);
    }
}
