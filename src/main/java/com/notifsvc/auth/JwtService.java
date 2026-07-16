package com.notifsvc.auth;

import com.notifsvc.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_USER_ID = "userId";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${notifsvc.security.jwt-secret}") String secret,
            @Value("${notifsvc.security.jwt-expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(JwtUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getUserId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TENANT_ID, user.getTenantId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(signingKey)
                .compact();
    }

    public JwtUserDetails parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long tenantId = claims.get(CLAIM_TENANT_ID, Long.class);
        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));

        return new JwtUserDetails(userId, claims.getSubject(), null, role, tenantId, true);
    }
}
