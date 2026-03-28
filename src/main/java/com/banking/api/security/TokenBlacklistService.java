package com.banking.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT token blacklist service.
 *
 * <p>This implementation stores revoked tokens in memory with their expiration times.
 * Tokens are automatically removed when they expire, preventing memory leaks.</p>
 *
 * <h3>Single-Instance Deployment</h3>
 * <p>For single-instance deployments, this implementation is sufficient and provides
 * proper token revocation support for logout and security scenarios.</p>
 *
 * <h3>Horizontal Scaling (Redis Backend)</h3>
 * <p>For multi-instance deployments, replace this with a Redis-backed implementation:</p>
 * <pre>
 * // Redis implementation example:
 * &#64;Service
 * public class RedisTokenBlacklistService {
 *     private final StringRedisTemplate redisTemplate;
 *
 *     public void blacklistToken(String tokenId, Instant expiresAt) {
 *         long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
 *         if (ttlSeconds > 0) {
 *             redisTemplate.opsForValue().set(
 *                 "blacklist:" + tokenId,
 *                 "revoked",
 *                 ttlSeconds,
 *                 TimeUnit.SECONDS
 *             );
 *         }
 *     }
 *
 *     public boolean isBlacklisted(String tokenId) {
 *         return Boolean.TRUE.equals(
 *             redisTemplate.hasKey("blacklist:" + tokenId)
 *         );
 *     }
 * }
 * </pre>
 *
 * <p>To enable Redis: Add spring-boot-starter-data-redis dependency and configure
 * spring.data.redis.url in application.yml</p>
 *
 * @see com.banking.api.security.JwtAuthenticationFilter
 * @see com.banking.api.service.AuthService
 */
@Service
@Slf4j
public class TokenBlacklistService {

    /**
     * Blacklist storage: token ID (jti) or token hash -> expiration instant.
     * Using ConcurrentHashMap for thread-safe concurrent access.
     */
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Blacklists a JWT token until its natural expiration.
     *
     * @param token The JWT token to blacklist
     */
    public void blacklistToken(String token) {
        try {
            Instant expiration = extractExpiration(token);
            String tokenId = getTokenIdentifier(token);

            if (Instant.now().isBefore(expiration)) {
                blacklist.put(tokenId, expiration);
                log.info("Token blacklisted: {} (expires at {})", tokenId, expiration);
            }
        } catch (Exception e) {
            // If we can't parse the token, store a hash of it
            String tokenHash = Integer.toHexString(token.hashCode());
            // Assume max refresh token expiration (7 days) if we can't parse
            Instant fallbackExpiry = Instant.now().plusMillis(7 * 24 * 60 * 60 * 1000L);
            blacklist.put(tokenHash, fallbackExpiry);
            log.warn("Token blacklisted with fallback expiry: {} (parse error: {})",
                    tokenHash, e.getMessage());
        }
    }

    /**
     * Checks if a token has been blacklisted.
     *
     * @param token The JWT token to check
     * @return true if the token is blacklisted and not yet expired
     */
    public boolean isBlacklisted(String token) {
        try {
            String tokenId = getTokenIdentifier(token);
            Instant expiry = blacklist.get(tokenId);

            if (expiry == null) {
                return false;
            }

            // Check if the blacklisted entry has expired
            if (Instant.now().isAfter(expiry)) {
                blacklist.remove(tokenId);
                log.debug("Removed expired blacklist entry: {}", tokenId);
                return false;
            }

            return true;
        } catch (Exception e) {
            // If we can't parse, check by hash
            String tokenHash = Integer.toHexString(token.hashCode());
            return blacklist.containsKey(tokenHash);
        }
    }

    /**
     * Scheduled cleanup of expired blacklist entries.
     * Runs every 5 minutes to prevent memory leaks.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanExpiredEntries() {
        int initialSize = blacklist.size();
        Instant now = Instant.now();

        blacklist.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));

        int removed = initialSize - blacklist.size();
        if (removed > 0) {
            log.info("Cleaned {} expired token blacklist entries (remaining: {})",
                    removed, blacklist.size());
        }
    }

    /**
     * Gets the current blacklist size (for monitoring).
     */
    public int getBlacklistSize() {
        return blacklist.size();
    }

    /**
     * Extracts a unique identifier for the token.
     * Uses the subject (username) + issuedAt time as identifier.
     */
    private String getTokenIdentifier(String token) {
        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();
        Date issuedAt = claims.getIssuedAt();
        return subject + ":" + (issuedAt != null ? issuedAt.getTime() : "unknown");
    }

    private Instant extractExpiration(String token) {
        return extractAllClaims(token).getExpiration().toInstant();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }
}
