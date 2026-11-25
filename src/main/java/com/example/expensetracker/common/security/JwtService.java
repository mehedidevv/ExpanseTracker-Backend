package com.example.expensetracker.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and verifies HS256 access tokens. Subject = user id. */
@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties props) {
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.issuer();
        this.accessTokenTtl = props.accessTokenTtl();
    }

    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim(EMAIL_CLAIM, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /** Returns the principal if the token is valid and not expired, otherwise empty. */
    public Optional<CurrentUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new CurrentUser(UUID.fromString(claims.getSubject()),
                    claims.get(EMAIL_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
