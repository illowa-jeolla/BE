package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class TravelCandidateCacheService {
    private static final String KEY_PREFIX = "travel:recommendation:";
    private static final String KEY_SUFFIX = ":candidates";
    private static final Duration TTL = Duration.ofHours(1);
    private static final TypeReference<List<TravelCandidateItem>> CANDIDATE_LIST_TYPE =
            new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    public TravelCandidateCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long requestId, List<TravelCandidateItem> candidates) {
        try {
            redisTemplate.opsForValue().set(key(requestId),
                    OBJECT_MAPPER.writeValueAsString(List.copyOf(candidates)), TTL);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.CANDIDATE_CACHE_UNAVAILABLE);
        }
    }

    public List<TravelCandidateItem> find(Long requestId) {
        try {
            String value = redisTemplate.opsForValue().get(key(requestId));
            if (value == null) {
                throw new TravelRecommendationException(
                        TravelRecommendationErrorCode.CANDIDATE_CACHE_NOT_FOUND);
            }
            return List.copyOf(OBJECT_MAPPER.readValue(value, CANDIDATE_LIST_TYPE));
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.CANDIDATE_CACHE_UNAVAILABLE);
        }
    }

    public void delete(Long requestId) {
        try {
            redisTemplate.delete(key(requestId));
        } catch (DataAccessException exception) {
            throw new TravelRecommendationException(
                    TravelRecommendationErrorCode.CANDIDATE_CACHE_UNAVAILABLE);
        }
    }

    String key(Long requestId) {
        return KEY_PREFIX + requestId + KEY_SUFFIX;
    }
}
