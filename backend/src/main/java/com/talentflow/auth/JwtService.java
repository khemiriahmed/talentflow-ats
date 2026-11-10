package com.talentflow.auth;

import com.talentflow.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(JwtProperties props) {
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = Duration.ofMinutes(props.accessTokenMinutes());
        this.refreshTtl = Duration.ofDays(props.refreshTokenDays());
    }

    public String generateAccessToken(String email, Role role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(refreshTtl)))
                .signWith(key)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public Claims parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    public long accessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    private Claims parse(String token, String expectedType) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Unexpected token type");
        }
        return claims;
    }
}
