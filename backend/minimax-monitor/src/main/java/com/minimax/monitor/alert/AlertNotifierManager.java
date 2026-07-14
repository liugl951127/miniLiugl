package com.minimax.monitor.alert;

import com.minimax.monitor.entity.AlertChannel;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.mapper.AlertChannelMapper;
import com.minimax.monitor.service.AlertTemplateResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 告警通知管理器 (V5.33 Day 18).
 *
 * 功能:
 *   - 启动时收集所有 AlertNotifier (Spring Bean)
 *   - 根据 channelType 路由到对应 notifier
 *   - 渠道配置来自 DB (alert_channel 表) + 内存缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertNotifierManager {

    private final List<AlertNotifier> notifiers;
    private final AlertChannelMapper channelMapper;
    private final AlertTemplateResolver templateResolver;

    /** channelType → AlertNotifier 实例 */
    private Map<String, AlertNotifier> notifierMap = new ConcurrentHashMap<>();

    /** 渠道列表缓存 */
    private volatile List<AlertChannel> cachedChannels = List.of();

    @PostConstruct
    public void init() {
        notifierMap = notifiers.stream()
                .collect(Collectors.toMap(AlertNotifier::channelType, Function.identity(), (a, b) -> a));
        log.info("AlertNotifierManager loaded: {}", notifierMap.keySet());
        refreshChannels();
    }

    /**
     * 通知所有已启用渠道。
     * 失败不阻断其他渠道。
     */
    public void notifyAll(AlertEvent event) {
        List<AlertChannel> channels = cachedChannels;
        for (AlertChannel ch : channels) {
            if (ch.getEnabled() == null || ch.getEnabled() == 0) continue;
            AlertNotifier n = notifierMap.get(ch.getChannelType());
            if (n == null) {
                log.debug("no notifier for channel type: {}", ch.getChannelType());
                continue;
            }
            try {
                n.send(event, ch.getConfig());
            } catch (Exception e) {
                log.warn("notifier {} failed for channel {}: {}",
                        ch.getChannelType(), ch.getName(), e.getMessage());
            }
        }
    }

    /** 刷新渠道缓存 */
    public void refreshChannels() {
        List<AlertChannel> all = channelMapper.selectList(null);
        cachedChannels = all.stream()
                .sorted(Comparator.comparingInt(c -> c.getPriority() != null ? c.getPriority() : 0))
                .toList();
        log.info("alert channels refreshed: {} channels", cachedChannels.size());
    }

    public List<AlertChannel> channels() {
        return cachedChannels;
    }

    /**
     * 发送测试消息到指定渠道 (Day 27).
     * 构建一个测试告警事件，发送给该渠道对应的 notifier。
     */
    public void sendTest(AlertChannel channel) {
        AlertNotifier n = notifierMap.get(channel.getChannelType());
        if (n == null) {
            throw new IllegalStateException("不支持的渠道类型: " + channel.getChannelType());
        }
        AlertEvent testEvent = new AlertEvent();
        testEvent.setId(-1L);
        testEvent.setRuleId(-1L);
        testEvent.setRuleName("[测试] 告警渠道连通性检测");
        testEvent.setSeverity("info");
        testEvent.setMetricName("test_metric");
        testEvent.setMetricValue(java.math.BigDecimal.ZERO);
        testEvent.setThreshold(java.math.BigDecimal.ZERO);
        testEvent.setStatus("firing");
        testEvent.setMessage("[测试消息] 您好，这是来自 MiniMax 平台的告警渠道测试消息。如果收到此消息，说明渠道配置正确。");
        testEvent.setFiredAt(java.time.LocalDateTime.now());
        // config 优先用 channel.config，其次用 channel.target
        String config = (channel.getConfig() != null && !channel.getConfig().isBlank())
                ? channel.getConfig()
                : channel.getTarget();
        // Day 28: 模板替换后发送
        String resolvedMessage = templateResolver.resolve(testEvent, channel.getTemplate());
        n.send(testEvent, config, resolvedMessage);
        log.info("[sendTest] 渠道测试已发送: id={} name={} type={}",
                channel.getId(), channel.getName(), channel.getChannelType());
    }
}
