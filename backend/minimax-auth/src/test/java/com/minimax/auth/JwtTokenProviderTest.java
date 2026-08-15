package com.minimax.auth;

import com.minimax.auth.jwt.JwtProperties;
import com.minimax.auth.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private JwtProperties props;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("test-secret-key-1234567890-abcdef-min-32-bytes");
        props.setIssuer("minimax-test");
        props.setAccessTtlSeconds(60L);
        props.setRefreshTtlSeconds(3600L);
        provider = new JwtTokenProvider(props);
    }

    @Test
    void issueAndParseAccessToken() {
        String token = provider.issueAccessToken(42L, "alice", List.of("ADMIN", "USER"));
        assertNotNull(token);
        Claims c = provider.parse(token);
        assertEquals(42L, Long.parseLong(c.getSubject()));
        assertEquals("alice", c.get("uname"));
        assertEquals(List.of("ADMIN", "USER"), c.get("roles"));
        assertNotNull(c.getExpiration());
    }

    @Test
    void refreshTokenHashIsDeterministic() {
        String raw = provider.issueRefreshToken();
        String h1 = provider.hashRefreshToken(raw);
        String h2 = provider.hashRefreshToken(raw);
        assertEquals(h1, h2);
        assertEquals(64, h1.length()); // SHA-256 hex
    }

    @Test
    void invalidTokenThrowsBizException() {
        assertThrows(RuntimeException.class, () -> provider.parse("not-a-token"));
    }

    @Test
    void extractUserIdAndRoles() {
        String token = provider.issueAccessToken(7L, "bob", List.of("USER"));
        Claims c = provider.parse(token);
        assertEquals(7L, provider.extractUserId(c));
        assertEquals(List.of("USER"), provider.extractRoles(c));
    }
}
