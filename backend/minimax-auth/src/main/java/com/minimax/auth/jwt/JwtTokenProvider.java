package com.minimax.auth.jwt;

import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 颁发 / 解析 / 校验。
 *
 * 关键设计：
 * 1. access token 短命(30min) + refresh token 长命(7d) 双 token 机制
 * 2. refresh token 不入 JWT，而是随机串 + 数据库存储（可吊销）
 * 3. 解析失败统一抛 BizException，便于全局异常处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecureRandom random = new SecureRandom();

    private SecretKey key() {
        // 把任意长度的 secret 规整为 32 字节，避免启动报错
        try {
            byte[] raw = props.getSecret().getBytes(StandardCharsets.UTF_8);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw);
            return Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 生成 access token（含 roles）。 */
    public String issueAccessToken(Long userId, String username, java.util.List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTtlSeconds());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .claim("uname", username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 refresh token。返回明文串，DB 存 SHA-256 哈希。
     * 这样即使 DB 泄漏，攻击者也无法直接拿来换 access token。
     */
    public String issueRefreshToken() {
        byte[] buf = new byte[48];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public String hashRefreshToken(String raw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 解析并校验签名 + 过期。失败抛 BizException。 */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .requireIssuer(props.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            throw new BizException(ResultCode.UNAUTHORIZED, "无效或过期的令牌");
        }
    }

    public Long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(Claims claims) {
        Object v = claims.get("roles");
        return v instanceof java.util.List ? (java.util.List<String>) v : java.util.List.of();
    }

    public Map<String, Object> introspect(String token) {
        Claims c = parse(token);
        return Map.of(
                "userId", c.getSubject(),
                "username", String.valueOf(c.get("uname")),
                "roles", c.get("roles"),
                "exp", c.getExpiration().toInstant().getEpochSecond()
        );
    }
}
