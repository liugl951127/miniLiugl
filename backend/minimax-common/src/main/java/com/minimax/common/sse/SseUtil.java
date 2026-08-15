package com.minimax.common.sse;

/**
 * V3.7.33+ SseUtil - SseResult 业务别名
 * 
 * 推荐新代码用 SseUtil (语义更清楚: 工具类)
 * 老代码继续用 SseResult (向后兼容)
 * 
 * 用法:
 *   SseUtil.sendBusiness(emitter, "thought", Map.of("content", "..."));
 *   SseUtil.sendDone(emitter);
 *   SseUtil.sendError(emitter, "err");
 *   SseUtil.complete(emitter);
 */
public final class SseUtil {

    private SseUtil() {}

    // === V3.7.33+ 业务别名 (委托 SseResult) ===
    
    /** 业务 event 入口 (自动查 type) */
    public static void sendBusiness(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String event, Object data) {
        SseResult.sendBusiness(emitter, event, data);
    }
    
    /** 标准 5 type 显式 */
    public static void sendCustom(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String event, String type, Object data) {
        SseResult.sendCustom(emitter, event, type, data);
    }
    
    /** 5 type 显式 */
    public static void send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String event, Object data) {
        SseResult.send(emitter, event, data);
    }
    
    /** 发错误 */
    public static void sendError(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String msg) {
        SseResult.sendError(emitter, msg);
    }
    
    /** 发完成 */
    public static void sendDone(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        SseResult.sendDone(emitter);
    }
    
    /** 完成 emitter */
    public static void complete(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        SseResult.complete(emitter);
    }
    
    /** 错误完成 */
    public static void completeWithError(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, Throwable t) {
        SseResult.completeWithError(emitter, t);
    }
}
