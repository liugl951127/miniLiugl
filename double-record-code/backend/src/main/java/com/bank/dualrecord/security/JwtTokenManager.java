package com.bank.dualrecord.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 管理器
 *
 * <p>修复 DRL-2026-002:JWT 撤销机制
 *
 * <p>新增功能:
 * <ul>
 *   <li>签发 token 携带 jti(唯一标识)
 *   <li>登出时将 jti 加入 Redis 黑名单
 *   <li>每次请求校验黑名单
 *   <li>Redis 过期时间 = token 剩余有效期
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenManager {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:7200}")
    private long expiration;

    @Value("${jwt.issuer:dual-record-platform}")
    private String issuer;

    private final StringRedisTemplate redis;
    private static final String BLACKLIST_PREFIX = "jwt:revoked:";

    public JwtTokenManager(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 签发 token
     */
    public String issue(String userId, String role, String branchId) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration * 1000L);

        return Jwts.builder()
            .id(jti)  // JWT ID,用于撤销
            .subject(userId)
            .issuer(issuer)
            .claim("role", role)
            .claim("branchId", branchId)
            .issuedAt(now)
            .expiration(exp)
            .signWith(getKey())
            .compact();
    }

    /**
     * 解析 token
     */
    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * 撤销 token(登出)
     */
    public void revoke(String token) {
        try {
            Claims claims = parse(token);
            String jti = claims.getId();
            Date exp = claims.getExpiration();
            long ttl = (exp.getTime() - System.currentTimeMillis()) / 1000;
            if (ttl > 0) {
                redis.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "1",
                    Duration.ofSeconds(ttl)
                );
                log.info("JWT 已撤销: jti={}, user={}, ttl={}s", jti, claims.getSubject(), ttl);
            }
        } catch (Exception e) {
            log.error("撤销 JWT 失败", e);
        }
    }

    /**
     * 校验 token 是否已被撤销
     */
    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + jti));
    }

    /**
     * 撤销某用户的所有 token(强制下线)
     */
    public void revokeAllForUser(String userId) {
        redis.opsForValue().set(
            BLACKLIST_PREFIX + "user:" + userId,
            "1",
            Duration.ofHours(24)
        );
        log.info("用户所有 token 已强制撤销: userId={}", userId);
    }

    /**
     * 校验某用户是否被强制下线
     */
    public boolean isUserRevoked(String userId) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + "user:" + userId));
    }

    /**
     * 刷新 token(轮换 jti)
     */
    public String refresh(String oldToken) {
        Claims claims = parse(oldToken);
        // 先撤销旧 token
        revoke(oldToken);
        // 签发新 token
        return issue(claims.getSubject(),
            claims.get("role", String.class),
            claims.get("branchId", String.class));
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpiration() {
        return expiration;
    }
}
