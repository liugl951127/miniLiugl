package com.bank.dualrecord.fabric.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 链码事件单元测试
 */
class ChainEventListenerTest {

    @Test
    void testSerialization() {
        ChainEventListener event = new ChainEventListener();
        event.setEventName("EvidenceSubmitted");
        event.setTxId("test-tx-001");
        event.setOrderId("12345");
        event.setBlockNumber(100);
        event.setEventTime(Instant.now());

        byte[] data = event.toBytes();
        assertNotNull(data);
        assertTrue(data.length > 0);

        ChainEventListener parsed = ChainEventListener.fromBytes(data);
        assertNotNull(parsed);
        assertEquals(event.getEventName(), parsed.getEventName());
        assertEquals(event.getTxId(), parsed.getTxId());
        assertEquals(event.getOrderId(), parsed.getOrderId());
        assertEquals(event.getBlockNumber(), parsed.getBlockNumber());
    }

    @Test
    void testParsePayload() {
        ChainEventListener event = new ChainEventListener();
        event.setPayload("{\"oldState\":\"VERIFIED\",\"newState\":\"RECORDING\",\"reason\":\"test\"}");

        var payload = event.parsePayload(java.util.Map.class);
        assertNotNull(payload);
        assertEquals("VERIFIED", payload.get("oldState"));
        assertEquals("RECORDING", payload.get("newState"));
    }

    @Test
    void testParseInvalidPayload() {
        ChainEventListener event = new ChainEventListener();
        event.setPayload("not valid json");
        var payload = event.parsePayload(java.util.Map.class);
        assertNull(payload);
    }
}
