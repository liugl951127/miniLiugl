package com.minimax.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Gateway 安全配置 (V5.3).
 *
 * 网关只负责路由 + 鉴权透传, 不做业务逻辑.
 * 业务鉴权由各微服务 (auth/chat/...) 自己的 SecurityConfig 处理.
 *
 * @since 2026-06
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/**").permitAll()  // 全部放行, 鉴权交给各微服务
            )
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // V5.3: 允许所有来源 (开发), 生产应限制
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",     // vite dev
                "http://localhost:5173",     // 旧端口
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "https://your-domain.com",   // 生产
                "https://api.your-domain.com"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}