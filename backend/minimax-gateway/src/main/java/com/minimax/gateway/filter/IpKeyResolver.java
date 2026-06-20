package com.minimax.gateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * IP 限流 Key 解析器 (V5.5).
 * 由 UserKeyResolver 引用 (不单独注册 Bean, 避免冲突)
 */
public class IpKeyResolver implements KeyResolver {
    public static final IpKeyResolver INSTANCE = new IpKeyResolver();

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            ip = xff.split(",")[0].trim();
        }
        return Mono.just("ip:" + ip);
    }
}