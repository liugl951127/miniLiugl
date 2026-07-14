package com.minimax.monitor.event;

import com.minimax.monitor.entity.AlertEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 告警触发事件 (Day 27).
 * AlertEngine 触发告警时发布，auth 模块监听并 WS 推送给在线用户。
 */
@Getter
public class AlertFiredEvent extends ApplicationEvent {

    private final AlertEvent alertEvent;

    public AlertFiredEvent(Object source, AlertEvent alertEvent) {
        super(source);
        this.alertEvent = alertEvent;
    }
}
