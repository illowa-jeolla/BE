package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TravelGuideDraftCacheService {
    private static final String KEY_PREFIX = "travel:guide:draft:";
    private static final String REFRESH_SUFFIX = ":refresh-used";
    private static final String MANUAL_POINTER_PREFIX = "travel:guide:manual:user:";
    private static final Duration TTL = Duration.ofHours(24);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final DefaultRedisScript<Long> REPLACE_MANUAL_SCRIPT =
            new DefaultRedisScript<>("""
                    local oldDraftId = redis.call('GET', KEYS[1])
                    if oldDraftId then
                        redis.call('DEL', ARGV[3] .. oldDraftId)
                        redis.call('DEL', ARGV[3] .. oldDraftId .. ARGV[4])
                    end
                    redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[2])
                    redis.call('SET', KEYS[1], ARGV[5], 'PX', ARGV[2])
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public TravelGuideDraftCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(TravelGuideDraft draft) {
        try {
            redisTemplate.opsForValue().set(key(draft.requestId()),
                    OBJECT_MAPPER.writeValueAsString(draft), TTL);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    public void replaceManual(TravelGuideDraft draft) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(draft);
            Long result = redisTemplate.execute(REPLACE_MANUAL_SCRIPT,
                    java.util.List.of(manualPointerKey(draft.userId()), key(draft.requestId())),
                    json, String.valueOf(TTL.toMillis()), KEY_PREFIX, REFRESH_SUFFIX,
                    String.valueOf(draft.requestId()));
            if (!Long.valueOf(1L).equals(result)) {
                throw unavailable();
            }
        } catch (JsonProcessingException | DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    public TravelGuideDraft find(Long draftId) {
        return findOptional(draftId).orElseThrow(() -> new TravelRecommendationException(
                TravelRecommendationErrorCode.DRAFT_NOT_FOUND));
    }

    public Optional<TravelGuideDraft> findOptional(Long draftId) {
        try {
            String value = redisTemplate.opsForValue().get(key(draftId));
            return value == null ? Optional.empty()
                    : Optional.of(OBJECT_MAPPER.readValue(value, TravelGuideDraft.class));
        } catch (JsonProcessingException | DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    public boolean useRefresh(Long draftId) {
        try {
            Boolean created = redisTemplate.opsForValue().setIfAbsent(
                    refreshKey(draftId), "1", TTL);
            return Boolean.TRUE.equals(created);
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    public boolean isRefreshUsed(Long draftId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey(draftId)));
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    public void delete(Long draftId) {
        try {
            redisTemplate.delete(java.util.List.of(key(draftId), refreshKey(draftId)));
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    private String key(Long draftId) {
        return KEY_PREFIX + draftId;
    }

    private String refreshKey(Long draftId) {
        return key(draftId) + REFRESH_SUFFIX;
    }

    private String manualPointerKey(Long userId) {
        return MANUAL_POINTER_PREFIX + userId + ":draft-id";
    }

    private TravelRecommendationException unavailable() {
        return new TravelRecommendationException(
                TravelRecommendationErrorCode.CANDIDATE_CACHE_UNAVAILABLE);
    }

    private TravelRecommendationException unavailable(Throwable cause) {
        return new TravelRecommendationException(
                TravelRecommendationErrorCode.CANDIDATE_CACHE_UNAVAILABLE, cause);
    }
}
