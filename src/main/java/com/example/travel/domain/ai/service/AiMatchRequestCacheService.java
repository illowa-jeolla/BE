package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.exception.AiMatchErrorCode;
import com.example.travel.domain.ai.exception.AiMatchException;
import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiMatchRequestCacheService {
    private static final String PREFIX = "ai:match:request:";
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public AiMatchRequestCacheService(StringRedisTemplate redisTemplate,
                                      @Value("${ai-match.request-ttl:1h}") Duration ttl) {
        this.redisTemplate = redisTemplate; this.ttl = ttl;
    }

    public void save(AiMatchRequestContext context) {
        try { redisTemplate.opsForValue().set(key(context.requestId()), MAPPER.writeValueAsString(context), ttl); }
        catch (Exception exception) { throw new AiMatchException(AiMatchErrorCode.CACHE_UNAVAILABLE, exception); }
    }

    public Optional<AiMatchRequestContext> find(UUID requestId) {
        try {
            String value = redisTemplate.opsForValue().get(key(requestId));
            return value == null ? Optional.empty() : Optional.of(MAPPER.readValue(value, AiMatchRequestContext.class));
        } catch (Exception exception) { throw new AiMatchException(AiMatchErrorCode.CACHE_UNAVAILABLE, exception); }
    }

    public void delete(UUID requestId) {
        try { redisTemplate.delete(key(requestId)); }
        catch (DataAccessException exception) { throw new AiMatchException(AiMatchErrorCode.CACHE_UNAVAILABLE, exception); }
    }

    private String key(UUID requestId) { return PREFIX + requestId; }
}
