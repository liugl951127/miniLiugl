package com.minimax.common.web;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * V5.10: HTTP 请求 metrics 自动采点过滤器.
 *
 * 自动记录每个 HTTP 请求:
 *  - minimax.http.requests.total    Counter (method, uri, status)
 *  - minimax.http.requests.duration Timer   (method, uri, status)
 *  - minimax.http.4xx.errors.total   Counter (method, uri, status)
 *  - minimax.http.5xx.errors.total   Counter (method, uri, status)
 *
 * 自动注册到 Micrometer (Prometheus 通过 /actuator/prometheus 抓取).
 *
 * Order=HIGHEST_PRECEDENCE+10 = 最早执行 (在 JwtAuth Filter 之前,
 * 因为一旦 JWT 失败抛 401 也要记录 metrics).
 *
 * @since V5.10
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MetricsFilter extends OncePerRequestFilter {

    private final MeterRegistry registry;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        long startNs = System.nanoTime();
        String method = req.getMethod();
        // URI 模板归一化 (避免 /api/v1/user/123 vs /api/v1/user/456 高基数)
        String uri = normalize(req.getRequestURI());

        try {
            chain.doFilter(req, res);
        } finally {
            try {
                int status = res.getStatus();
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                Tags tags = Tags.of(
                        "method", method,
                        "uri", uri,
                        "status", String.valueOf(status));

                // 通用计数
                registry.counter("minimax.http.requests.total", tags).increment();
                registry.timer("minimax.http.requests.duration", tags)
                        .record(durationMs, TimeUnit.MILLISECONDS);

                // 错误计数分流 (方便 Grafana 直接 rate(minimax_http_4xx_errors_total))
                if (status >= 500) {
                    registry.counter("minimax.http.5xx.errors.total", tags).increment();
                } else if (status >= 400) {
                    registry.counter("minimax.http.4xx.errors.total", tags).increment();
                }
            } catch (Exception ex) {
                log.warn("metrics record failed: {}", ex.getMessage());
            }
        }
    }

    /**
     * URI 归一化: 把 /api/v1/user/123 → /api/v1/user/{id}
     * 防止高基数 metric (一个用户一个 label 值).
     *
     * 简化: 只替换纯数字段为 {id}.
     */
    private String normalize(String uri) {
        if (uri == null) return "unknown";
        // /api/v1/sessions/abc-123-uuid/messages/456 → /api/v1/sessions/{id}/messages/{id}
        return uri.replaceAll("/[0-9a-fA-F-]{8,}(?=/|$)", "/{id}")
                  .replaceAll("/\\d+(?=/|$)", "/{id}");
    }
}
