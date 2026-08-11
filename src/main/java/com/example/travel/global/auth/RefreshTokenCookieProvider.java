package com.example.travel.global.auth;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieProvider {
    private final JwtProperties properties;

    public RefreshTokenCookieProvider(JwtProperties properties) { this.properties = properties; }

    public ResponseCookie create(String token) {
        return base(token).maxAge(Duration.ofMillis(properties.refreshTokenExpiration())).build();
    }

    public ResponseCookie expire() { return base("").maxAge(Duration.ZERO).build(); }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSecure() ? "None" : "Lax")
                .path("/api/v1/auth");
    }
}
