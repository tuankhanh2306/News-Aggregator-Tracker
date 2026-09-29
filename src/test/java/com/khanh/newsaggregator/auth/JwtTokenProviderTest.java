package com.khanh.newsaggregator.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    void generateToken_shouldProduceValidJwt() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "ROLE_USER");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("test@example.com", jwtTokenProvider.extractEmail(token));
        assertEquals(1L, jwtTokenProvider.extractUserId(token));
    }

    @Test
    void validateToken_invalidToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.structure"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void validateToken_tamperedToken_shouldReturnFalse() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "ROLE_USER");
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtTokenProvider.validateToken(tampered));
    }
}
