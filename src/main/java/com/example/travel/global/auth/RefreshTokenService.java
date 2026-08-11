package com.example.travel.global.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RefreshTokenService {
    private static final String PREFIX = "RT:";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2])
                return 1
            end
            return 0
            """, Long.class);
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

    public boolean rotate(Long userId, String currentToken, String newToken) {
        Long result = redisTemplate.execute(ROTATE_SCRIPT, List.of(PREFIX + userId),
                currentToken, newToken, Long.toString(properties.refreshTokenExpiration()));
        return Long.valueOf(1L).equals(result);
    }

    public void delete(Long userId) { redisTemplate.delete(PREFIX + userId); }
}
