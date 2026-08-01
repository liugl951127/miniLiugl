package com.bank.dualrecord.fabric.event;

import com.bank.dualrecord.fabric.event.handler.ChainEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 链码事件 Kafka 消费者
 *
 * <p>订阅: fabric.{channel}.{eventName} 多个 topic
 * <p>消费者组: chain-event-consumer
 * <p>幂等保证: 落库时唯一索引 (chain_tx_id, event_name)
 *
 * <p>设计要点:
 * <ul>
 *   <li>手动 ACK(业务处理完才提交 offset)
 *   <li>失败重试 3 次 → 死信队列
 *   <li>事件分发到对应 Handler(责任链)
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainEventConsumer {

    private final List<ChainEventHandler> handlers;
    private final com.bank.dualrecord.fabric.event.ChainEventAuditService auditService;

    @KafkaListener(
        topics = {
            "fabric.dual-record-channel.EvidenceSubmitted",
            "fabric.dual-record-channel.StateChanged",
            "fabric.dual-record-channel.NodeResultAppended",
            "fabric.dual-record-channel.ContractSigned",
            "fabric.dual-record-channel.AuditRecorded",
            "fabric.dual-record-channel.EvidenceFinalized"
        },
        groupId = "chain-event-consumer",
        concurrency = "3"
    )
    public void onEvent(
        @Payload byte[] data,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment ack
    ) {
        ChainEventListener event = ChainEventListener.fromBytes(data);
        if (event == null) {
            log.warn("事件反序列化失败: topic={}, offset={}", topic, offset);
            ack.acknowledge();
            return;
        }

        log.info("收到链码事件: name={}, orderId={}, txId={}, block={}, offset={}/{}",
            event.getEventName(), event.getOrderId(), event.getTxId(),
            event.getBlockNumber(), partition, offset);

        try {
            // 1. 落库(幂等)
            auditService.persist(event);

            // 2. 业务分发
            dispatch(event);

            // 3. ACK
            ack.acknowledge();
        } catch (Exception e) {
            log.error("事件处理失败: {}", e.getMessage(), e);
            // 不 ACK,等待重试 / 进入死信
            // 实际生产应使用 DeadLetterPublishingRecoverer
        }
    }

    /**
     * 事件分发(责任链模式)
     */
    private void dispatch(ChainEventListener event) {
        for (ChainEventHandler handler : handlers) {
            if (handler.supports(event.getEventName())) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    log.error("Handler 处理失败: {} - {}", handler.getClass().getSimpleName(), e.getMessage());
                    // 单个 handler 失败不影响其他
                }
            }
        }
    }
}
