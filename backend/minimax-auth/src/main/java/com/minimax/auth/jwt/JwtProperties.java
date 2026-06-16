package com.minimax.auth.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。从 application.yml 注入。
 *
 * 生产环境务必把 secret 放到环境变量或 KMS。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minimax.jwt")
public class JwtProperties {
    /** HS256 密钥，至少 32 字节。 */
    private String secret = "minimax-default-secret-please-override-in-production-32+";
    /** 签发方 */
    private String issuer = "minimax-platform";
    /** Access token 有效期(秒) 默认 30 分钟 */
    private Long accessTtlSeconds = 60L * 30L;
    /** Refresh token 有效期(秒) 默认 7 天 */
    private Long refreshTtlSeconds = 60L * 60L * 24L * 7L;
    /** 请求头名称 */
    private String header = "Authorization";
    /** Token 前缀 */
    private String prefix = "Bearer ";
}
