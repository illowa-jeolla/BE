package com.example.travel.global.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenService {
    private static final String PREFIX = "RT:";
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties properties;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void save(Long userId, String token) {
        redisTemplate.opsForValue().set(PREFIX + userId, token,
                Duration.ofMillis(properties.refreshTokenExpiration()));
    }

    public boolean matches(Long userId, String token) {
        return token.equals(redisTemplate.opsForValue().get(PREFIX + userId));
    }

    public void delete(Long userId) { redisTemplate.delete(PREFIX + userId); }
}
