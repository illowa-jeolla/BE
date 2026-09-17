package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TravelRecommendationRequestCacheService {
    private static final String KEY_PREFIX = "travel:recommendation:";
    private static final String SEQUENCE_KEY = "travel:recommendation:sequence";
    private static final Duration TTL = Duration.ofHours(24);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private final StringRedisTemplate redisTemplate;

    public TravelRecommendationRequestCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Long nextId() {
        try {
            Long id = redisTemplate.opsForValue().increment(SEQUENCE_KEY);
            if (id == null) throw unavailable();
            return id;
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    public void save(TravelRecommendationContext request) {
        try {
            redisTemplate.opsForValue().set(key(request.getId()),
                    OBJECT_MAPPER.writeValueAsString(request), TTL);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw unavailable();
        }
    }

    public TravelRecommendationContext find(Long requestId) {
        try {
            String value = redisTemplate.opsForValue().get(key(requestId));
            if (value == null) throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.REQUEST_NOT_FOUND);
            return OBJECT_MAPPER.readValue(value, TravelRecommendationContext.class);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw unavailable();
        }
    }

    private String key(Long requestId) { return KEY_PREFIX + requestId + ":context"; }
    private TravelRecommendationException unavailable() {
        return new TravelRecommendationException(
                TravelRecommendationErrorCode.CANDIDATE_CACHE_UNAVAILABLE);
    }
}
