package com.bank.dualrecord.fabric.event;

import com.bank.dualrecord.mapper.ChainEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 链码事件落库服务
 *
 * <p>幂等保证:UNIQUE INDEX uk_chain_tx (chain_tx_id, event_name)
 * <p>重复事件自动忽略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChainEventAuditService {

    private final ChainEventMapper chainEventMapper;

    /**
     * 落库(幂等)
     *
     * @return true=新事件入库,false=重复事件
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean persist(ChainEventListener event) {
        try {
            chainEventMapper.insert(event);
            log.debug("事件已落库: txId={}, name={}", event.getTxId(), event.getEventName());
            return true;
        } catch (DuplicateKeyException e) {
            log.debug("事件重复(幂等): txId={}, name={}", event.getTxId(), event.getEventName());
            return false;
        }
    }

    /**
     * 标记已处理
     */
    public void markProcessed(String txId, String eventName) {
        chainEventMapper.markProcessed(txId, eventName);
    }
}
