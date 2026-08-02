package com.minimax.common.sse;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * V3.7.26+ SSE Result 包装工具
 *
 * 目的: 跟 HTTP Result<T> 统一, 前端 useBusinessStream 统一处理
 * - 成功 (code=0): {code:0, message:"success", data: <业务对象>, timestamp}
 * - 失败 (code=1): {code:1, message:"err", data: {message:"..."}, timestamp}
 *
 * 用法:
 *   SseEmitter emitter = new SseEmitter(120_000L);
 *   SseResult.send(emitter, "content", Map.of("content", "hello"));
 *   SseResult.sendError(emitter, "stream failed");
 *   SseResult.complete(emitter);
 */
public final class SseResult {

    private SseResult() {}

    /**
     * 发成功事件 (自动包 Result, code=0)
     * 5 type 业务: start / content / tool_call / source / done
     */
    public static void send(SseEmitter emitter, String event, Object data) {
        send(emitter, event, data, 0, "success");
    }

    /**
     * V3.7.28+ 业务自定义 event 类型
     * 用于 agent 业务: step / thought / observation / final / planner-start / planner-plan / executor-step 等
     * 跟标准 5 type 走一样的 Result 包装, 业务用 useBusinessStream 5 type 别名兼容 (chunk/toolcall/src/finish/err)
     *
     * @param emitter SseEmitter 实例
     * @param event 事件名 (业务自定义, 例: "step", "thought", "observation")
     * @param type 标准 type (例: "content" / "tool_call" / "source" / "done" / "error")
     *              决定前端 useBusinessStream 路由到哪个 onXxx
     * @param data 业务数据
     */
    public static void sendCustom(SseEmitter emitter, String event, String type, Object data) {
        // V3.7.28+ 自定义 event + 标准 type 组合
        // 实际发出去的结构: {code:0, message:"success", data:{type:"content", event:"step", content:"..."}, timestamp}
        // 前端 useBusinessStream: 剥 Result.data, 看 data.type 路由, data.event 给业务用
        try {
            java.util.Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put("event", event);
            wrapped.put("type", type);
            wrapped.putAll((java.util.Map) data);
            send(emitter, event, wrapped);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 发自定义 code + message 事件
     */
    public static void send(SseEmitter emitter, String event, Object data, int code, String message) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("code", code);
            payload.put("message", message);
            payload.put("data", data);
            payload.put("timestamp", System.currentTimeMillis());
            emitter.send(SseEmitter.event()
                .name(event)
                .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            // 连接断开, 不抛
        }
    }

    /**
     * 发错误事件 (code=1, data 包含 message)
     */
    public static void sendError(SseEmitter emitter, String errorMessage) {
        Map<String, Object> errData = new LinkedHashMap<>();
        errData.put("message", errorMessage);
        send(emitter, "error", errData, 1, errorMessage);
    }

    /**
     * 发完成事件 (code=0)
     */
    public static void sendDone(SseEmitter emitter) {
        send(emitter, "done", Map.of("status", "finished"), 0, "success");
    }

    /**
     * 完成 emitter
     */
    public static void complete(SseEmitter emitter) {
        try { emitter.complete(); } catch (Exception ignore) {}
    }

    /**
     * 错误完成 (SseEmitter.completeWithError 会触发客户端 error 事件)
     */
    public static void completeWithError(SseEmitter emitter, Throwable t) {
        try { emitter.completeWithError(t); } catch (Exception ignore) {}
    }
}
