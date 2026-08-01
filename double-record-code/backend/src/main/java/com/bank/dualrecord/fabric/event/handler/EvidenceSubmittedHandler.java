package com.bank.dualrecord.fabric.event.handler;

import com.bank.dualrecord.fabric.event.ChainEventListener;
import com.bank.dualrecord.model.Order;
import com.bank.dualrecord.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 证据提交事件处理器
 *
 * <p>链上提交证据后:
 * <ol>
 *   <li>更新业务订单的链上 TxID
 *   <li>触发业务侧状态机推进(已核验 → 话术执行中)
 *   <li>通知业务方
 * </ol>
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class EvidenceSubmittedHandler implements ChainEventHandler {

    private final OrderService orderService;

    @Override
    public boolean supports(String eventName) {
        return "EvidenceSubmitted".equals(eventName);
    }

    @Override
    public void handle(ChainEventListener event) {
        String orderId = event.getOrderId();
        if (orderId == null) {
            log.warn("EvidenceSubmitted 事件缺少 orderId: txId={}", event.getTxId());
            return;
        }
        log.info("处理证据提交事件: orderId={}, txId={}", orderId, event.getTxId());
        try {
            Order order = orderService.getById(Long.parseLong(orderId));
            // TODO: 更新业务订单的 blockChainTx + 状态推进
            // order.setBlockChainTx(event.getTxId());
            // order.setState(OrderStateVerified);
            // orderService.updateById(order);
        } catch (Exception e) {
            log.error("处理 EvidenceSubmitted 失败: orderId={}, error={}", orderId, e.getMessage());
        }
    }
}
