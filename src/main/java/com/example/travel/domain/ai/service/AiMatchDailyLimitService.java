package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.exception.AiMatchErrorCode;
import com.example.travel.domain.ai.exception.AiMatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class AiMatchDailyLimitService {
    private static final String PREFIX = "ai:match:daily:";
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                redis.call('DECR', KEYS[1])
                return 0
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final int dailyLimit;

    public AiMatchDailyLimitService(StringRedisTemplate redisTemplate, Clock clock,
                                    @Value("${ai-match.daily-limit:2}") int dailyLimit) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    public void acquire(Long userId) {
        Instant now = clock.instant();
        LocalDate today = now.atZone(KOREA_ZONE).toLocalDate();
        Instant nextMidnight = today.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant();
        long ttlMillis = Math.max(1, Duration.between(now, nextMidnight).toMillis());

        try {
            Long result = redisTemplate.execute(ACQUIRE_SCRIPT, List.of(key(userId, today)),
                    Long.toString(ttlMillis), Integer.toString(dailyLimit));
            if (result == null || result == 0) {
                throw new AiMatchException(AiMatchErrorCode.DAILY_LIMIT_EXCEEDED);
            }
        } catch (AiMatchException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new AiMatchException(AiMatchErrorCode.CACHE_UNAVAILABLE, exception);
        }
    }

    private String key(Long userId, LocalDate date) {
        return PREFIX + userId + ":" + date;
    }
}
