package com.minimax.model;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.provider.MockAdapter;
import com.minimax.model.vo.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockAdapterTest {

    private final MockAdapter adapter = new MockAdapter();

    @Test
    void codeIsMock() {
        assertEquals("mock", adapter.code());
    }

    @Test
    void chatReturnsContent() {
        ChatRequest req = new ChatRequest();
        req.setModel("test-model");
        req.setMessages(List.of(Map.of("role","user","content","hello world")));

        ChatResponse resp = adapter.chat(null, null, req);
        assertNotNull(resp);
        assertNotNull(resp.getContent());
        assertTrue(resp.getContent().contains("hello world"));
        assertEquals("test-model", resp.getModel());
        assertEquals("mock", resp.getProviderCode());
        assertEquals("stop", resp.getFinishReason());
        assertNotNull(resp.getTotalTokens());
        assertTrue(resp.getTotalTokens() > 0);
    }

    @Test
    void chatHandlesEmptyMessages() {
        ChatRequest req = new ChatRequest();
        req.setModel("test-model");
        req.setMessages(List.of());
        ChatResponse resp = adapter.chat(null, null, req);
        assertNotNull(resp);
        assertNotNull(resp.getContent());
    }
}
