package com.minimax.model;

import com.minimax.model.provider.MockAdapter;
import com.minimax.model.provider.ModelProviderFactory;
import com.minimax.model.provider.OpenAiCompatibleAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelProviderFactoryTest {

    private final ModelProviderFactory factory = new ModelProviderFactory(
            List.of(new OpenAiCompatibleAdapter(), new MockAdapter()));

    @Test
    void getOpenAi() {
        assertTrue(factory.get("openai") instanceof OpenAiCompatibleAdapter);
    }

    @Test
    void getOpenAiCompatProtocols() {
        // minimax / ollama / zhipu / qwen / deepseek 都用 openai 协议 → 拿 OpenAiCompatibleAdapter
        for (String code : List.of("minimax","ollama","zhipu","qwen","deepseek")) {
            assertTrue(factory.get(code) instanceof OpenAiCompatibleAdapter,
                    "expected OpenAI adapter for " + code);
        }
    }

    @Test
    void getUnknownFallsBackToMock() {
        assertTrue(factory.get("nonexistent") instanceof MockAdapter);
    }

    @Test
    void getMockExplicit() {
        assertTrue(factory.get("mock") instanceof MockAdapter);
    }
}
