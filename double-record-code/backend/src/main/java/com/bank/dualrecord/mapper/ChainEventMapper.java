package com.bank.dualrecord.mapper;

import com.bank.dualrecord.fabric.event.ChainEventListener;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 链码事件 Mapper
 */
@Mapper
public interface ChainEventMapper {

    @Insert("""
        INSERT INTO t_chain_event (
            event_name, chain_tx_id, order_id, session_id, channel_id,
            block_num, payload, event_time, received_at
        ) VALUES (
            #{eventName}, #{txId}, #{orderId}, #{sessionId}, #{channelId},
            #{blockNumber}, #{payload}, #{eventTime}, #{receivedAt}
        )
    """)
    int insert(ChainEventListener event);

    @Update("""
        UPDATE t_chain_event
        SET processed = 1, process_remark = 'processed by consumer'
        WHERE chain_tx_id = #{txId} AND event_name = #{eventName}
    """)
    int markProcessed(@Param("txId") String txId, @Param("eventName") String eventName);
}
