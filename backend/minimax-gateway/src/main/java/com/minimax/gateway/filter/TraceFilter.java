// =============================================================
// MiniMax - TraceId 全链路追踪 Filter (V5.8 + V5.14 升级)
// =============================================================
//
// 位置: minimax-gateway 模块 (WebFlux 响应式)
// 作用: 每个 HTTP 请求注入唯一 traceId, 用于:
//       1. 跨服务日志关联 (MDC 上下文)
//       2. 客户端报错时回传 (响应头 X-Trace-Id)
//       3. 排错时一搜 traceId 就能串起所有日志
//       4. OpenTelemetry 兼容 (W3C traceparent 标准格式, V5.14 升级)
//
// Order = -200 (最高优先级, 比 JwtAuth 更早, 因为鉴权失败也要带 traceId)
// =============================================================

// 当前包: gateway filter 目录
package com.minimax.gateway.filter;

// Lombok - 自动生成 log 字段
import lombok.extern.slf4j.Slf4j;
// Spring Cloud Gateway - 过滤器链
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
// Spring Cloud Gateway - 全局过滤器接口
import org.springframework.cloud.gateway.filter.GlobalFilter;
// Spring - 排序接口
import org.springframework.core.Ordered;
// Spring HTTP 响应式 - 响应对象
import org.springframework.http.server.reactive.ServerHttpResponse;
// Spring - 注册为 Bean
import org.springframework.stereotype.Component;
// Spring WebFlux - 服务端 Web 交换
import org.springframework.web.server.ServerWebExchange;
// Reactor - 响应式 Mono
import reactor.core.publisher.Mono;

// Java 标准 - UUID 用于生成 traceId
import java.util.UUID;

/**
 * V5.8 TraceId Filter + V5.14 W3C traceparent 升级.
 *
 * 每个请求生成唯一 traceId (或复用 header X-Trace-Id), 用于:
 *  - 跨服务日志追踪 (MDC)
 *  - 响应头 X-Trace-Id 返回给客户端
 *  - nginx/gateway 错误排查
 *  - OpenTelemetry 跨服务追踪 (W3C traceparent)
 *
 * Order=-200 (最高, 在 JwtAuth 前)
 */
@Slf4j                                                                       // Lombok 生成 log
@Component                                                                   // 注册为 Spring Bean
public class TraceFilter implements GlobalFilter, Ordered {                  // 实现 GlobalFilter + Ordered

    // 响应头 / 请求头 常量名 (供其他模块 / 前端统一引用)
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    // SLF4J MDC 键 (logback 配置 %X{traceId} 输出)
    public static final String MDC_KEY = "traceId";

    /**
     * 每个 HTTP 请求都会经过此方法 (在 JwtAuth 之前执行).
     *
     * @param exchange WebFlux exchange
     * @param chain    过滤器链
     * @return Mono<Void> 响应式
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 复用客户端传入的 traceId (e.g. 前端每次请求新生成), 否则生成新的 16 位 hex
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            // 生成 UUID, 去 -, 取前 16 hex 字符 (短而够用)
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // 2. 把 traceId 写入响应头 (客户端可读, 用于排查)
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(TRACE_ID_HEADER, traceId);

        // 3. V5.14: W3C traceparent 头, 兼容 OpenTelemetry / Jaeger
        // 格式: "00-{32hex traceId}-{16hex spanId}-{2hex flags}"
        // - 00: W3C version
        // - 32hex traceId: 我们 16 位 hex traceId 左补 0 到 32 位
        // - 16hex spanId: 新生成 (gateway 是 trace 的根节点, span 自己生成)
        // - 01: sampled flag (1 = 采样)
        final String traceIdPadded = (traceId + "00000000000000000000000000000000").substring(0, 32);  // 补 0 到 32 位
        final String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        final String traceparent = "00-" + traceIdPadded + "-" + spanId + "-01";
        // 4. 注入到下游请求头 (下游微服务用 OTel SDK 自动解析 traceparent)
        exchange.getRequest().mutate()
                .header("traceparent", traceparent)
                .build();

        // 5. 记录开始时间, 用于响应后计算耗时
        long startNs = System.nanoTime();
        // 提取 path / method / response (供 then() 闭包用, Java 闭包变量需 final)
        final String path = exchange.getRequest().getURI().getPath();
        final String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN";
        final ServerHttpResponse resp = response;
        final long t0 = startNs;
        final String tid = traceId;

        // 6. 放行到下游, 然后 (then) 记录访问日志
        return chain.filter(exchange)
                .then(Mono.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        // 6.1 计算耗时 (纳秒 → 毫秒)
                        long costMs = (System.nanoTime() - t0) / 1_000_000;
                        // 6.2 提取响应状态码
                        int status = resp.getStatusCode() != null ? resp.getStatusCode().value() : 0;
                        // 6.3 根据状态码选不同日志级别 (5xx 错误 / 4xx 警告 / 正常 info)
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

    /**
     * 过滤器执行顺序.
     *
     * @return -200 (最高优先级, 比 JwtAuth (-100) 和限流 (0) 都早)
     */
    @Override
    public int getOrder() {
        return -200;  // 最高, 早于 JwtAuth (-100) 和限流
    }
}
