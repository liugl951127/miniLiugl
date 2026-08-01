package com.bank.dualrecord.fabric.event.handler;

import com.bank.dualrecord.fabric.event.ChainEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 审计记录事件处理器
 */
@Slf4j
@Component
@Order(40)
public class AuditRecordedHandler implements ChainEventHandler {

    @Override
    public boolean supports(String eventName) {
        return "AuditRecorded".equals(eventName);
    }

    @Override
    public void handle(ChainEventListener event) {
        log.info("审计事件: txId={}, orderId={}", event.getTxId(), event.getOrderId());
        // TODO: 业务侧审计日志表同步
    }
}
