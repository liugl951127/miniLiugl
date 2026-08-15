package com.minimax.common.request;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求日志中间件。
 *
 * 记录:
 *  - traceId (UUID, 注入到 response header)
 *  - method / path / query / status / duration
 *  - 慢请求 (> 1000ms 标记)
 *  - 错误请求 (>= 500 标记)
 *
 * 不入库 (避免侵入), 由具体模块继承后写 DB.
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLogFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_ATTR = "traceId";
    public static final long SLOW_THRESHOLD_MS = 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        long t0 = System.currentTimeMillis();
        String traceId = req.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        req.setAttribute(TRACE_ID_ATTR, traceId);
        resp.setHeader(TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(req, resp);
        } finally {
            long dur = System.currentTimeMillis() - t0;
            int status = resp.getStatus();
            boolean slow = dur >= SLOW_THRESHOLD_MS;
            boolean err = status >= 500;
            String path = req.getRequestURI();
            String method = req.getMethod();
            String clientIp = getClientIp(req);
            String userAgent = req.getHeader("User-Agent");

            if (slow || err) {
                log.warn("[{}] {} {} -> {} {}ms (slow={}, err={}) ua={} ip={}",
                        traceId, method, path, status, dur, slow, err, truncate(userAgent, 60), clientIp);
            } else {
                log.debug("[{}] {} {} -> {} {}ms", traceId, method, path, status, dur);
            }
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return req.getRemoteAddr();
    }

    private String truncate(String s, int n) {
        return s == null ? "" : (s.length() > n ? s.substring(0, n) : s);
    }
}
