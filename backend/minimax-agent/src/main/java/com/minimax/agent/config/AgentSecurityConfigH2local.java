package com.minimax.agent.config;

import com.minimax.common.config.CorsUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Agent 模块沙箱模式 Security 配置 (V7.0)
 *
 * - 所有请求 → AgentH2localMockAuthFilter 注入 mock 用户
 * - /health → permitAll
 */
@Configuration
@Profile("h2local")
public class AgentSecurityConfigH2local {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AgentSecurityConfigH2local.class);

    private final AgentH2localMockAuthFilter agentH2localMockAuthFilter;

    public AgentSecurityConfigH2local(AgentH2localMockAuthFilter agentH2localMockAuthFilter) {
        this.agentH2localMockAuthFilter = agentH2localMockAuthFilter;
    }

    @Value("${server.port:8088}")
    private int port;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return CorsUtils.buildCorsConfigurationSource("/**");
    }

    @Bean
    public SecurityFilterChain h2localSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("[Security/H2local] 加载沙箱安全配置，所有请求 permitAll");
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/health", "/actuator/**").permitAll()
                    .anyRequest().permitAll()
            )
            // AgentH2localMockAuthFilter 在最前面注入 mock 用户
            .addFilterBefore(agentH2localMockAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
