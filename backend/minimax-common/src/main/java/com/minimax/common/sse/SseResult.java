package com.minimax.common.sse;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * V3.7.26+ SSE Result 包装工具 (V3.7.30+ 加通用路由表)
 *
 * 目的: 跟 HTTP Result<T> 统一, 前端 useBusinessStream 统一处理
 * - 成功 (code=0): {code:0, message:"success", data: <业务对象>, timestamp}
 * - 失败 (code=1): {code:1, message:"err", data: {message:"..."}, timestamp}
 *
 * V3.7.30+ EVENT_TO_TYPE 通用路由表:
 *   业务只关心 event (start/thought/tool-call/observation/final 等)
 *   SseResult 自动根据路由表找 type (content/tool_call/source/error)
 *   前端 useBusinessStream 按 type 路由到 onXxx
 *
 * 用法:
 *   SseEmitter emitter = new SseEmitter(120_000L);
 *   SseResult.send(emitter, "content", Map.of("content", "hello"));  // 5 type 标准
 *   SseResult.sendBusiness(emitter, "thought", Map.of("content", "..."));  // 业务自动路由
 *   SseResult.sendError(emitter, "stream failed");
 *   SseResult.complete(emitter);
 */
public final class SseResult {

    private SseResult() {}

    /**
     * V3.7.30+ 业务 event → 标准 5 type 路由表
     * 业务 sendBusiness 时自动查, 业务不用关心 type
     *
     * 标准 5 type: content / tool_call / source / done / error
     */
    public static final Map<String, String> EVENT_TO_TYPE = new HashMap<>();
    static {
        // content 类型 (默认)
        EVENT_TO_TYPE.put("start", "content");
        EVENT_TO_TYPE.put("step-start", "content");
        EVENT_TO_TYPE.put("thought", "content");
        EVENT_TO_TYPE.put("observation", "content");
        EVENT_TO_TYPE.put("final", "content");
        EVENT_TO_TYPE.put("planner-start", "content");
        EVENT_TO_TYPE.put("planner-plan", "content");
        EVENT_TO_TYPE.put("executor-step", "content");
        EVENT_TO_TYPE.put("executor-result", "content");
        EVENT_TO_TYPE.put("heartbeat", "content");
        EVENT_TO_TYPE.put("chunk", "content");      // 兼容老 chat chunk
        
        // tool_call 类型
        EVENT_TO_TYPE.put("tool-call", "tool_call");
        EVENT_TO_TYPE.put("toolcall", "tool_call");  // 兼容老别名
        
        // source 类型
        EVENT_TO_TYPE.put("tools", "source");
        EVENT_TO_TYPE.put("src", "source");          // 兼容老别名
        
        // error 类型
        EVENT_TO_TYPE.put("error", "error");
        EVENT_TO_TYPE.put("step-error", "error");
        EVENT_TO_TYPE.put("err", "error");           // 兼容老别名
    }

    /**
     * V3.7.30+ 根据 event 查 type (查不到默认 content)
     */
    public static String getType(String event) {
        return EVENT_TO_TYPE.getOrDefault(event, "content");
    }

    /**
     * 发成功事件 (自动包 Result, code=0)
     * 标准 5 type 业务: content / tool_call / source / done
     */
    public static void send(SseEmitter emitter, String event, Object data) {
        send(emitter, event, data, 0, "success");
    }

    /**
     * V3.7.30+ 业务自定义 event 入口 (自动路由 type)
     * 业务不用关心 type, 只传 event + data
     * SseResult 根据 EVENT_TO_TYPE 自动找 type
     *
     * 用法:
     *   SseResult.sendBusiness(emitter, "thought", Map.of("content", "thinking..."));
     *   SseResult.sendBusiness(emitter, "tool-call", Map.of("name", "...", "args", {...}));
     *   SseResult.sendBusiness(emitter, "tools", Map.of("tools", [...]));  // source 类型
     *   SseResult.sendBusiness(emitter, "step-error", Map.of("message", "..."));  // error 类型
     */
    public static void sendBusiness(SseEmitter emitter, String event, Object data) {
        String type = getType(event);
        sendCustom(emitter, event, type, data);
    }

    /**
     * V3.7.28+ 业务自定义 event + type 显式
     * 一般用 sendBusiness 即可, 只有路由表没有的 event 才用这个
     */
    public static void sendCustom(SseEmitter emitter, String event, String type, Object data) {
        try {
            Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put("event", event);
            wrapped.put("type", type);
            if (data instanceof Map) {
                wrapped.putAll((Map) data);
            } else {
                wrapped.put("value", data);
            }
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
