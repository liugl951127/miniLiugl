package com.minimax.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 审计上下文（ThreadLocal）。
 *
 * <p>由 {@link AuditRequestInterceptor} 在请求入口填充，
 * AOP Aspect 从这里读取用户/IP/UA 等信息，不需要穿透方法签名。</p>
 *
 * @author MiniMax
 * @since V6.8.2
 */
@Slf4j
public class AuditContext {

    private static final ThreadLocal<AuditInfo> CTX = new ThreadLocal<>();

    public static void set(AuditInfo info) { CTX.set(info); }

    public static AuditInfo get() { return CTX.get(); }

    public static void clear() { CTX.remove(); }

    /** 从当前线程获取 userId（未登录返回 null） */
    public static Long getUserId() {
        AuditInfo info = CTX.get();
        return info != null ? info.userId : null;
    }

    /** 从当前线程获取 username（未登录返回 "anonymous"） */
    public static String getUsername() {
        AuditInfo info = CTX.get();
        return info != null && info.username != null ? info.username : "anonymous";
    }

    /** 从当前线程获取 IP（无则返回 "-"） */
    public static String getIp() {
        AuditInfo info = CTX.get();
        return info != null && info.ip != null ? info.ip : "-";
    }

    /** 从当前线程获取 User-Agent */
    public static String getUserAgent() {
        AuditInfo info = CTX.get();
        return info != null ? info.userAgent : null;
    }

    public static void setFromRequest(HttpServletRequest req, Long userId, String username) {
        AuditInfo info = new AuditInfo();
        info.userId = userId;
        info.username = username;
        info.ip = req.getRemoteAddr();
        info.userAgent = req.getHeader("User-Agent");
        CTX.set(info);
    }

    public static class AuditInfo {
        public Long userId;
        public String username;
        public String ip;
        public String userAgent;
        public String traceId;
    }
}
