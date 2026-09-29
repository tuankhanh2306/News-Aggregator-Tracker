package com.khanh.newsaggregator.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.expirationMs = expirationMs;
        SecretKey secretKey;
        try {
            byte[] keyBytes = java.util.HexFormat.of().parseHex(secret);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
        this.key = secretKey;
    }

    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object userId = getClaims(token).get("userId");
        if (userId instanceof Number num) {
            return num.longValue();
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
