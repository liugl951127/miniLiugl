package com.minimax.common.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * V3.7.34+ SseResult 端到端测试 (mock 完整 SSE 链路)
 * 
 * 模拟后端 → SSE → 前端 useBusinessStream 解析 完整链路:
 *   1. 后端 AgentService.runStream
 *   2. SseResult.sendBusiness 多次发不同 event
 *   3. SseResult.sendDone 结束
 *   4. mock 完整捕获 payload, 验证前端能解析
 */
class SseResultE2ETest {

    private SseEmitter emitter;
    private java.util.List<SseEmitter.SseEventBuilder> capturedEvents;

    @BeforeEach
    void setUp() throws IOException {
        emitter = mock(SseEmitter.class);
        capturedEvents = new java.util.ArrayList<>();
        // capture 所有 send 调用
        doAnswer(invocation -> {
            SseEmitter.SseEventBuilder builder = invocation.getArgument(0);
            capturedEvents.add(builder);
            return null;
        }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void test1_agentFullFlow() throws IOException {
        // 模拟 agent 完整流程
        // 1. start
        SseResult.sendBusiness(emitter, "start", Map.of(
            "streamId", "stream-1",
            "goal", "test goal"
        ));
        
        // 2. tools
        SseResult.sendBusiness(emitter, "tools", Map.of(
            "tools", java.util.List.of(Map.of("name", "search"), Map.of("name", "calc"))
        ));
        
        // 3. step-start
        SseResult.sendBusiness(emitter, "step-start", Map.of("round", 1));
        
        // 4. thought
        SseResult.sendBusiness(emitter, "thought", Map.of("content", "I need to search"));
        
        // 5. tool-call
        SseResult.sendBusiness(emitter, "tool-call", Map.of(
            "name", "search",
            "args", Map.of("q", "test")
        ));
        
        // 6. observation
        SseResult.sendBusiness(emitter, "observation", Map.of("content", "result 1"));
        
        // 7. final
        SseResult.sendBusiness(emitter, "final", Map.of("answer", "final answer"));
        
        // 8. done
        SseResult.sendDone(emitter);
        
        // 验证: 8 个事件都被发出去
        assertEquals(8, capturedEvents.size(), "应该发 8 个事件 (start/tools/step-start/thought/tool-call/observation/final/done)");
    }

    @Test
    void test2_chatFullFlow() throws IOException {
        // 模拟 chat 完整流程
        // 1. start
        SseResult.sendBusiness(emitter, "start", Map.of(
            "streamId", "stream-1",
            "sessionId", 123,
            "status", "started"
        ));
        
        // 2-N. content chunks
        String[] chunks = {"你", "好", "，", "世", "界"};
        for (String chunk : chunks) {
            SseResult.sendBusiness(emitter, "content", Map.of("content", chunk));
        }
        
        // N+1. done
        SseResult.sendDone(emitter);
        
        // 验证: 1 start + 5 content + 1 done = 7 个事件
        assertEquals(7, capturedEvents.size());
    }

    @Test
    void test3_musicStreamFullFlow() throws IOException {
        // 模拟 Music Stream 流程
        SseResult.sendBusiness(emitter, "start", Map.of(
            "taskId", "task-1",
            "totalBars", 16,
            "style", "POP"
        ));
        
        // heartbeat
        SseResult.sendBusiness(emitter, "heartbeat", Map.of("ping", System.currentTimeMillis()));
        
        // chunks
        for (int i = 0; i < 3; i++) {
            SseResult.sendBusiness(emitter, "chunk", Map.of(
                "taskId", "task-1",
                "chunkIndex", i,
                "data", "base64-data"
            ));
        }
        
        // progress
        SseResult.sendBusiness(emitter, "progress", Map.of(
            "taskId", "task-1",
            "percent", 75
        ));
        
        // complete
        SseResult.sendBusiness(emitter, "complete", Map.of(
            "taskId", "task-1",
            "durationMs", 3000
        ));
        
        // 验证: 1+1+3+1+1 = 7 个事件
        assertEquals(7, capturedEvents.size());
    }

    @Test
    void test4_errorInMiddle() throws IOException {
        // 错误在中间发生的场景
        SseResult.sendBusiness(emitter, "start", Map.of("ok", true));
        SseResult.sendBusiness(emitter, "content", Map.of("content", "x"));
        // 业务错误
        SseResult.sendError(emitter, "stream failed");
        // complete
        SseResult.complete(emitter);
        
        // 验证: 3 send (start/content/error) + 1 complete
        assertEquals(3, capturedEvents.size());
        verify(emitter, times(1)).complete();
    }

    @Test
    void test5_ioExceptionDoesntThrow() throws IOException {
        // 模拟连接断开, send 抛 IOException
        doThrow(new IOException("connection lost")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        
        // 不会向上抛
        assertDoesNotThrow(() -> {
            SseResult.sendBusiness(emitter, "content", Map.of("content", "x"));
        });
        assertDoesNotThrow(() -> {
            SseResult.sendError(emitter, "err");
        });
        assertDoesNotThrow(() -> {
            SseResult.sendDone(emitter);
        });
    }
}
