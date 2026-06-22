package com.minimax.monitor.alert;

import com.minimax.monitor.entity.AlertChannel;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.mapper.AlertChannelMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
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
public class AlertNotifierManager {

    @Resource
    private List<AlertNotifier> notifiers;

    @Resource
    private AlertChannelMapper channelMapper;

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
}
