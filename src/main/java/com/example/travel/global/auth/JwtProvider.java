package com.example.travel.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {
    private static final String TOKEN_TYPE = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String role) {
        return createToken(userId, ACCESS, role, properties.accessTokenExpiration());
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, REFRESH, null, properties.refreshTokenExpiration());
    }

    private String createToken(Long userId, String type, String role, long expiration) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim(TOKEN_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration));
        if (role != null) builder.claim("role", role);
        return builder.signWith(signingKey).compact();
    }

    public boolean isValidAccessToken(String token) {
        try {
            return ACCESS.equals(claims(token).get(TOKEN_TYPE, String.class));
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isValidRefreshToken(String token) {
        try {
            return REFRESH.equals(claims(token).get(TOKEN_TYPE, String.class));
        } catch (Exception ignored) {
            return false;
        }
    }

    public Long userId(String token) { return claims(token).get("userId", Long.class); }
    public String role(String token) { return claims(token).get("role", String.class); }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
