package com.bank.dualrecord.fabric.event.handler;

import com.bank.dualrecord.fabric.event.ChainEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 状态变更事件处理器
 *
 * <p>链上订单状态变更时,同步业务侧 + 触发通知(短信/钉钉/邮件)
 */
@Slf4j
@Component
@Order(20)
public class StateChangedHandler implements ChainEventHandler {

    @Override
    public boolean supports(String eventName) {
        return "StateChanged".equals(eventName);
    }

    @Override
    public void handle(ChainEventListener event) {
        // 解析 payload
        Map<String, Object> payload = event.parsePayload(Map.class);
        if (payload == null) return;

        String orderId = (String) payload.get("orderId");
        String oldState = (String) payload.get("oldState");
        String newState = (String) payload.get("newState");
        String reason = (String) payload.get("reason");

        log.info("订单状态变更: orderId={}, {} -> {}, reason={}", orderId, oldState, newState, reason);

        // 1. 同步业务侧状态
        // 2. 异常态触发风控告警
        if ("CANCELLED".equals(newState) || "FAILED".equals(newState)) {
            log.warn("订单异常状态: orderId={}, state={}, reason={}", orderId, newState, reason);
            // TODO: 钉钉/邮件告警
        }
        // 3. COMPLETED 状态触发归档
        if ("COMPLETED".equals(newState)) {
            log.info("订单完成,触发归档: orderId={}", orderId);
            // TODO: 异步归档(冷存储)
        }
    }
}
