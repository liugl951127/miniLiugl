package com.bank.dualrecord.fabric.event.handler;

import com.bank.dualrecord.fabric.event.ChainEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 合同签署事件处理器
 */
@Slf4j
@Component
@Order(30)
public class ContractSignedHandler implements ChainEventHandler {

    @Override
    public boolean supports(String eventName) {
        return "ContractSigned".equals(eventName);
    }

    @Override
    public void handle(ChainEventListener event) {
        log.info("合同签署事件: txId={}, payload={}", event.getTxId(), event.getPayload());
        // TODO: 业务侧合同状态更新 + 通知
    }
}
