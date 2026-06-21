package com.minimax.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * V5.8 TraceId Filter
 *
 * 每个请求生成唯一 traceId (或复用 header X-Trace-Id), 用于:
 *  - 跨服务日志追踪 (MDC)
 *  - 响应头 X-Trace-Id 返回给客户端
 *  - nginx/gateway 错误排查
 *
 * Order=-200 (最高, 在 JwtAuth 前)
 */
@Slf4j
@Component
public class TraceFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 复用客户端传入的 traceId, 否则生成新的
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // 2. 注入到下游请求头 (微服务可读) + 响应头
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(TRACE_ID_HEADER, traceId);

        long startNs = System.nanoTime();
        final String path = exchange.getRequest().getURI().getPath();
        final String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN";
        final ServerHttpResponse resp = response;
        final long t0 = startNs;
        final String tid = traceId;

        return chain.filter(exchange)
                .then(Mono.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        long costMs = (System.nanoTime() - t0) / 1_000_000;
                        int status = resp.getStatusCode() != null ? resp.getStatusCode().value() : 0;
                        if (status >= 500) {
                            log.error("{} {} -> {} ({}ms) traceId={}", method, path, status, costMs, tid);
                        } else if (status >= 400) {
                            log.warn("{} {} -> {} ({}ms) traceId={}", method, path, status, costMs, tid);
                        } else {
                            log.info("{} {} -> {} ({}ms) traceId={}", method, path, status, costMs, tid);
                        }
                    }
                }));
    }

    @Override
    public int getOrder() {
        return -200;  // 最高, 早于 JwtAuth (-100) 和限流
    }
}