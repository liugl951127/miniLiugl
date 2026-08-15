package com.minimax.common.config;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 工具类 (V6.8.2 安全修复).
 *
 * 修复: 不再使用 allowedOriginPatterns("*") + allowCredentials(true) 的冲突组合.
 * 生产必须通过 CORS_ORIGINS 环境变量指定具体域名，格式: "https://a.com,https://b.com"
 * 开发环境默认仅允许 localhost.
 */
public class CorsUtils {

    private static final List<String> DEV_ORIGINS = List.of(
            "http://localhost:*", "http://127.0.0.1:*"
    );

    public static CorsConfigurationSource buildCorsConfigurationSource(String pathPattern) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> allowedOrigins = resolveAllowedOrigins();
        cfg.setAllowedOriginPatterns(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(pathPattern, cfg);
        return source;
    }

    private static List<String> resolveAllowedOrigins() {
        String originsEnv = System.getenv("CORS_ORIGINS");
        if (originsEnv != null && !originsEnv.isBlank()) {
            return Arrays.asList(originsEnv.trim().split(","));
        }
        return DEV_ORIGINS;
    }
}
