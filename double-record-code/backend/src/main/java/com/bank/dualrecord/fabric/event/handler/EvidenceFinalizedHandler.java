package com.bank.dualrecord.fabric.event.handler;

import com.bank.dualrecord.fabric.event.ChainEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 证据终结事件处理器
 *
 * <p>证据归档锁定后:
 * <ol>
 *   <li>触发 AI 质检(异步)
 *   <li>触发监管报送
 *   <li>通知客户
 * </ol>
 */
@Slf4j
@Component
@Order(5)
public class EvidenceFinalizedHandler implements ChainEventHandler {

    @Override
    public boolean supports(String eventName) {
        return "EvidenceFinalized".equals(eventName);
    }

    @Override
    public void handle(ChainEventListener event) {
        log.info("证据已终结: orderId={}, txId={}", event.getOrderId(), event.getTxId());
        // 触发后续流程(异步)
        // - AI 质检
        // - 监管报送
        // - 客户通知
    }
}
