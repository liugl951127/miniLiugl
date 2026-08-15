package com.minimax.gateway.fallback;

import com.minimax.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * V5.7 Resilience4j Fallback Controller
 *
 * 当下游微服务熔断 (5xx 50% / timeout 5s) 时, gateway 把请求 forward 到这里,
 * 返回友好降级响应而不是 500.
 *
 * 路由: yml 中 `fallbackUri: forward:/fallback` 触发.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/**")
    public Mono<Result<Void>> fallback() {
        log.warn("[Fallback] triggered - service degraded");
        return Mono.just(Result.<Void>fail(503, "服务暂不可用, 已自动降级. 请稍后重试."));
    }
}