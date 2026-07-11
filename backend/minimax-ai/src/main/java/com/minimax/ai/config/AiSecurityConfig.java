package com.minimax.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * AI 模块安全配置 (V2.8.3) — 开关模式
 *
 * <h3>用法</h3>
 * <pre>{@code
 *   # application.yml 或环境变量
 *   minimax:
 *     ai:
 *       security:
 *         enabled: true   # 主模式 (Gateway 走 JWT 鉴权, 本服务做最后兜底)
 *         enabled: false  # 独立/演示模式 (不鉴权, 业务逻辑内部判断)
 * }</pre>
 *
 * <h3>主模式 (enabled=true)</h3>
 * <ul>
 *   <li>JwtAuthenticationFilter (在 common) 已解析 token, 写 SecurityContext</li>
 *   <li>本配置只放行 /api/ai/health (探针), 其他要求已认证</li>
 *   <li>Gateway → AI: 内部信任, header 传递用户 ID</li>
 * </ul>
 *
 * <h3>独立模式 (enabled=false, 默认)</h3>
 * <ul>
 *   <li>关闭所有 Security 拦截</li>
 *   <li>适合: 演示 / 嵌入式 / 单机 / 边缘计算</li>
 * </ul>
 */
@Configuration
public class AiSecurityConfig {

    /**
     * 是否启用鉴权
     * 默认 false (独立模式); 主模式通过 application.yml 设为 true
     */
    @Value("${minimax.ai.security.enabled:false}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (securityEnabled) {
            // 主模式: 关闭 csrf, 放行 health, 其他要求已认证
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/ai/health", "/api/ai/info", "/actuator/**").permitAll()
                    .anyRequest().permitAll()  // 由 JwtAuthenticationFilter + @PreAuthorize 控制
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        } else {
            // 独立模式: 完全放行
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.disable());
        }
        return http.build();
    }
}
