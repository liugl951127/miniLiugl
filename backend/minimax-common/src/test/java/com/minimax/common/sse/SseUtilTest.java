package com.minimax.common.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * V3.7.33+ SseUtil 单元测试 (别名类, 委托 SseResult)
 * 验证 6 个方法都正确委托
 */
class SseUtilTest {

    private SseEmitter emitter;

    @BeforeEach
    void setUp() {
        emitter = mock(SseEmitter.class);
    }

    @Test
    void test1_sendBusiness() throws IOException {
        SseUtil.sendBusiness(emitter, "thought", Map.of("content", "x"));
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void test2_sendCustom() throws IOException {
        SseUtil.sendCustom(emitter, "my-event", "content", Map.of("data", "x"));
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void test3_send() throws IOException {
        SseUtil.send(emitter, "content", Map.of("content", "x"));
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void test4_sendError() throws IOException {
        SseUtil.sendError(emitter, "err msg");
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void test5_sendDone() throws IOException {
        SseUtil.sendDone(emitter);
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void test6_complete() {
        SseUtil.complete(emitter);
        verify(emitter, times(1)).complete();
        
        SseUtil.completeWithError(emitter, new RuntimeException("test"));
        verify(emitter, times(1)).completeWithError(any(Throwable.class));
    }
}
