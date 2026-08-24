package com.example.travel.domain.auth.google.service;

import com.example.travel.global.auth.JwtProperties;
import com.example.travel.domain.auth.google.exception.GoogleErrorCode;
import com.example.travel.domain.auth.google.exception.GoogleException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class GoogleLoginStateService {
    public static final String COOKIE_NAME = "google_oauth_state";
    private static final String PREFIX = "GOOGLE_STATE:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public GoogleLoginStateService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    public String issue(HttpServletResponse response) {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + state, "1", TTL);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(state, TTL).toString());
        return state;
    }

    public void consume(String state, String cookieState, HttpServletResponse response) {
        if (state == null || !state.equals(cookieState)
                || redisTemplate.opsForValue().getAndDelete(PREFIX + state) == null) {
            throw new GoogleException(GoogleErrorCode.INVALID_STATE);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth/google/callback")
                .maxAge(maxAge)
                .build();
    }
}
