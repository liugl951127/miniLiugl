package com.bank.dualrecord.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Actuator 端点安全配置
 *
 * <p>修复 DRL-2026-003:Actuator 端点暴露
 *
 * <p>策略:
 * <ul>
 *   <li>仅暴露 health + info
 *   <li>IP 白名单限制(只允许内网/运维网段)
 *   <li>禁止显示详细信息
 *   <li>详细端点(env/heapdump/threaddump)需鉴权
 * </ul>
 */
@Slf4j
@Configuration
public class ActuatorSecurityConfig {

    @Value("${actuator.allowed-ips:127.0.0.1,10.0.0.0/8,192.168.0.0/16,172.16.0.0/12}")
    private String allowedIpsConfig;

    /**
     * Actuator 端点专属安全过滤链
     *
     * <p>Spring Security 2.7 用 @Order 控制顺序
     */
    @Bean
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        List<String> allowedIps = Arrays.asList(allowedIpsConfig.split(","));
        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
                .anyRequest().hasIpAddress(allowedIpsConfig)
            )
            .addFilterBefore(new ActuatorIpFilter(allowedIps), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .csrf(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Actuator IP 白名单过滤器
     */
    public static class ActuatorIpFilter extends OncePerRequestFilter {

        private final List<String> allowedIps;

        public ActuatorIpFilter(List<String> allowedIps) {
            this.allowedIps = allowedIps;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
            String ip = getClientIp(req);
            if (isAllowed(ip)) {
                chain.doFilter(req, resp);
            } else {
                log.warn("Actuator 访问被拒: ip={}, uri={}", ip, req.getRequestURI());
                resp.setStatus(403);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Forbidden: Actuator access denied from IP " + ip);
            }
        }

        private boolean isAllowed(String ip) {
            for (String allowed : allowedIps) {
                if (allowed.contains("/")) {
                    // CIDR
                    if (matchCidr(ip, allowed)) return true;
                } else {
                    if (allowed.equals(ip)) return true;
                }
            }
            return false;
        }

        private boolean matchCidr(String ip, String cidr) {
            // 简化 CIDR 匹配(只支持 /8 /16 /24)
            try {
                String[] parts = cidr.split("/");
                String baseIp = parts[0];
                int prefix = Integer.parseInt(parts[1]);

                String[] ipParts = ip.split("\\.");
                String[] baseParts = baseIp.split("\\.");

                if (ipParts.length != 4 || baseParts.length != 4) return false;

                int fullBytes = prefix / 8;
                int remainingBits = prefix % 8;

                for (int i = 0; i < fullBytes; i++) {
                    if (!ipParts[i].equals(baseParts[i])) return false;
                }
                if (remainingBits > 0 && fullBytes < 4) {
                    int ipByte = Integer.parseInt(ipParts[fullBytes]);
                    int baseByte = Integer.parseInt(baseParts[fullBytes]);
                    int mask = 0xFF << (8 - remainingBits) & 0xFF;
                    if ((ipByte & mask) != (baseByte & mask)) return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private String getClientIp(HttpServletRequest req) {
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
    }
}
