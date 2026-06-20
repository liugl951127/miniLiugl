package com.minimax.gateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 用户限流 Key 解析器 (V5.5).
 * 已登录用户按 X-User-Id 限流, 未登录回退到 IP.
 */
@Component("userKeyResolver")
public class UserKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId == null) {
            return IpKeyResolver.INSTANCE.resolve(exchange);
        }
        return Mono.just("user:" + userId);
    }
}