package com.example.travel.global.auth;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final RefreshTokenService service = new RefreshTokenService(redisTemplate,
            new JwtProperties("test-secret-key-must-be-at-least-32-bytes-long", 60_000, 120_000, false));

    @Test
    @SuppressWarnings("unchecked")
    void rotatesOnlyWhenStoredTokenMatches() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("RT:7")),
                eq("current"), eq("new"), eq("120000"))).thenReturn(1L);

        assertThat(service.rotate(7L, "current", "new")).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsAlreadyRotatedToken() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("RT:7")),
                eq("stale"), eq("new"), eq("120000"))).thenReturn(0L);

        assertThat(service.rotate(7L, "stale", "new")).isFalse();
    }
}
