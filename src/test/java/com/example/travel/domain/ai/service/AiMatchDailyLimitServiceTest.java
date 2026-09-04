package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.exception.AiMatchException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiMatchDailyLimitServiceTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T14:30:00Z"), ZoneOffset.UTC);
    private final AiMatchDailyLimitService service = new AiMatchDailyLimitService(redisTemplate, clock, 2);

    @Test
    void acquiresUsingKoreaCalendarDate() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);

        service.acquire(7L);

        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("ai:match:daily:7:2026-09-03")),
                eq("1800000"), eq("2"));
    }

    @Test
    void rejectsWhenDailyLimitIsExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.acquire(7L))
                .isInstanceOf(AiMatchException.class);
    }
}
