package com.bank.dualrecord.security;

import com.bank.dualrecord.config.RateLimitConfig;
import com.bank.dualrecord.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 限流拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimit;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 按 IP + 接口限流
        String ip = getClientIp(request);
        String key = ip + ":" + request.getRequestURI();
        if (!rateLimit.tryAcquire(key, 20)) {
            log.warn("限流触发: key={}", key);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<Void> r = ApiResponse.fail(429, "请求过于频繁,请稍后再试");
            response.getWriter().write(objectMapper.writeValueAsString(r));
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
