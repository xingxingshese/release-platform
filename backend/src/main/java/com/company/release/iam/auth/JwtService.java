package com.company.release.iam.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与校验（HS256）。Token 携带 userId/username/permissions。
 */
@Service
public class JwtService {

    public record TokenClaims(Long userId, String username, List<String> permissions) {
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_PERMISSIONS = "perms";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.ttl:PT8H}") Duration ttl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.ttl = ttl;
    }

    public String issue(Long userId, String username, List<String> permissions) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_PERMISSIONS, permissions)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public TokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long uid = claims.get(CLAIM_USER_ID, Long.class);
            List<String> perms = (List<String>) claims.get(CLAIM_PERMISSIONS, List.class);
            return new TokenClaims(uid, claims.getSubject(),
                    perms == null ? List.of() : List.copyOf(perms));
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("token expired");
        } catch (Exception e) {
            throw new InvalidTokenException("invalid token");
        }
    }
}
