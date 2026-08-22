package com.minimax.ai.marketplace.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.sse.SseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 工具封装 (T1-backend-orchestrator)
 *
 * <p>复用 {@link SseUtil}, 串行调用 {@code SseUtil.sendBusiness}。
 * 单例 ObjectMapper 复用以减少分配。
 */
@Slf4j
public final class SseEmitterUtil {

    private SseEmitterUtil() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 业务 event 入口 (委托 SseUtil) */
    public static void send(SseEmitter emitter, String event, Object data) {
        try {
            SseUtil.sendBusiness(emitter, event, data);
        } catch (Exception e) {
            // 客户端断开导致 send 抛异常, 不应中断编排
            if (!isClientGone(e)) {
                log.debug("[sse] send {} 异常: {}", event, e.getMessage());
            }
        }
    }

    /** error event */
    public static void sendError(SseEmitter emitter, String message) {
        try {
            SseUtil.sendError(emitter, message);
        } catch (Exception e) {
            log.debug("[sse] sendError 异常: {}", e.getMessage());
        }
    }

    private static boolean isClientGone(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("broken pipe") || m.contains("connection reset")
                || m.contains("client abort") || m.contains("已断开");
    }
}
