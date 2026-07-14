package com.minimax.monitor.event;

import com.minimax.monitor.entity.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 告警事件发布器 (Day 27).
 * AlertEngine 调用此组件发布 AlertFiredEvent，供其他模块订阅。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * 发布告警触发事件。
     * 调用方：AlertEngine。
     */
    public void publish(AlertEvent alertEvent) {
        log.debug("[AlertEventPublisher] publishing alert: {}", alertEvent.getMessage());
        publisher.publishEvent(new AlertFiredEvent(this, alertEvent));
    }
}
