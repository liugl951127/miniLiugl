package com.minimax.model;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.provider.MockAdapter;
import com.minimax.model.provider.OpenAiCompatibleAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StreamingTest {

    @Test
    void mockStreamProducesChunks() {
        MockAdapter adapter = new MockAdapter();
        ChatRequest req = new ChatRequest();
        req.setModel("test-model");
        req.setMessages(List.of(Map.of("role","user","content","hi")));

        StringBuilder collected = new StringBuilder();
        AtomicInteger count = new AtomicInteger(0);
        OpenAiCompatibleAdapter.StreamResult result = adapter.streamChat(req,
                chunk -> {
                    collected.append(chunk);
                    count.incrementAndGet();
                },
                new AtomicBoolean(false));

        assertNotNull(result);
        assertEquals("stop", result.finishReason());
        assertTrue(count.get() > 5, "should produce multiple chunks, got " + count.get());
        assertTrue(result.content().length() > 0);
        assertTrue(collected.toString().contains("data:") || collected.toString().contains("choices"));
    }

    @Test
    void mockStreamRespectsCancelFlag() throws Exception {
        MockAdapter adapter = new MockAdapter();
        ChatRequest req = new ChatRequest();
        req.setModel("test");
        req.setMessages(List.of(Map.of("role","user","content","x")));

        AtomicBoolean stop = new AtomicBoolean(false);
        // 100ms 后取消
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            stop.set(true);
        }).start();

        OpenAiCompatibleAdapter.StreamResult result = adapter.streamChat(req, c -> {}, stop);

        assertEquals("cancelled", result.finishReason());
    }

    @Test
    void mockStreamFullText() {
        MockAdapter adapter = new MockAdapter();
        ChatRequest req = new ChatRequest();
        req.setModel("demo");
        req.setMessages(List.of(Map.of("role","user","content","hello world")));

        StringBuilder fullText = new StringBuilder();
        adapter.streamChat(req, chunk -> {
            // chunk 是原始 JSON: {"choices":[{"index":0,"delta":{"content":"X"}}]}
            // 使用 Jackson 解析提取 content
            try {
                com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<?,?> parsed = m.readValue(chunk, Map.class);
                List<?> choices = (List<?>) parsed.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?,?> first = (Map<?,?>) choices.get(0);
                    Map<?,?> delta = (Map<?,?>) first.get("delta");
                    if (delta != null && delta.get("content") != null) {
                        fullText.append(delta.get("content"));
                    }
                }
            } catch (Exception ignore) {}
        }, new AtomicBoolean(false));

        assertTrue(fullText.length() > 0, "should collect text, got: " + fullText);
    }
}
