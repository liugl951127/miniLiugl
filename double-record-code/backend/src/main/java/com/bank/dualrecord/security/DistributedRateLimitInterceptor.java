package com.bank.dualrecord.security;

import com.bank.dualrecord.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 分布式限流拦截器
 *
 * <p>修复 DRL-2026-001:替代 Guava RateLimiter
 *
 * <p>默认规则(可在 application.yml 覆盖):
 * <ul>
 *   <li>登录接口:每 IP 5 次/分钟
 *   <li>验证码:每 IP 3 次/分钟
 *   <li>写接口:每 IP 30 次/分钟
 *   <li>读接口:每 IP 120 次/分钟
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedRateLimitInterceptor implements HandlerInterceptor {

    private final DistributedRateLimiter rateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.login:5}")
    private int loginLimit;

    @Value("${rate-limit.captcha:3}")
    private int captchaLimit;

    @Value("${rate-limit.write:30}")
    private int writeLimit;

    @Value("${rate-limit.read:120}")
    private int readLimit;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!enabled) return true;

        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 判定限流规则
        Rule rule = resolveRule(uri, method);
        if (rule == null) return true;  // 不限流

        String key = "rl:" + rule.name + ":" + ip;
        DistributedRateLimiter.RateLimitResult result =
            rateLimiter.tryAcquireSliding(key, rule.limit, rule.windowSec);

        // 设置响应头(便于客户端看到限流状态)
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));

        if (!result.allowed) {
            log.warn("限流触发: ip={}, uri={}, rule={}, current={}/{}",
                ip, uri, rule.name, result.current, result.limit);

            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(rule.windowSec));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> body = ApiResponse.fail(429,
                "请求过于频繁,请 " + rule.windowSec + " 秒后重试");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        return true;
    }

    /**
     * 判定限流规则
     */
    private Rule resolveRule(String uri, String method) {
        // 登录接口
        if (uri.contains("/auth/login") || uri.contains("/auth/register")) {
            return new Rule("login", loginLimit, 60);
        }
        // 验证码
        if (uri.contains("/captcha") || uri.contains("/sms-code")) {
            return new Rule("captcha", captchaLimit, 60);
        }
        // 写操作
        if (Arrays.asList("POST", "PUT", "DELETE", "PATCH").contains(method.toUpperCase())) {
            return new Rule("write", writeLimit, 60);
        }
        // 读操作
        return new Rule("read", readLimit, 60);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static class Rule {
        final String name;
        final int limit;
        final int windowSec;
        Rule(String name, int limit, int windowSec) {
            this.name = name;
            this.limit = limit;
            this.windowSec = windowSec;
        }
    }
}
