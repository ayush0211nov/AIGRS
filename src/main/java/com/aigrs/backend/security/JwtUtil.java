package com.aigrs.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiry}") long accessExpiry,
            @Value("${jwt.refresh-expiry}") long refreshExpiry
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMs = accessExpiry * 1000;
        this.refreshExpiryMs = refreshExpiry * 1000;
    }

    public String generateAccessToken(UUID userId, String role, UUID orgId) {
        return buildToken(userId, role, orgId, accessExpiryMs);
    }

    public String generateRefreshToken(UUID userId, String role, UUID orgId) {
        return buildToken(userId, role, orgId, refreshExpiryMs);
    }

    private String buildToken(UUID userId, String role, UUID orgId, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "role", role,
                        "org_id", orgId.toString()
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(key)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractOrgId(String token) {
        return extractClaims(token).get("org_id", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    /** Returns remaining TTL in seconds — used for Redis blacklist expiry */
    public long getRemainingTtlSeconds(String token) {
        Date expiration = extractExpiration(token);
        long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }
}
