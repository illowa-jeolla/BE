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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TravelGuideDraftCacheService {
    private static final String KEY_PREFIX = "travel:guide:draft:";
    private static final String REFRESH_SUFFIX = ":refresh-used";
    private static final String MANUAL_POINTER_PREFIX = "travel:guide:manual:user:";
    private static final String USER_DRAFT_INDEX_PREFIX = "travel:guide:user:";
    private static final String USER_DRAFT_INDEX_SUFFIX = ":drafts";
    private static final Duration TTL = Duration.ofHours(24);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final DefaultRedisScript<Long> SAVE_SCRIPT =
            new DefaultRedisScript<>("""
                    redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
                    redis.call('ZADD', KEYS[2], ARGV[3], ARGV[3])
                    redis.call('PEXPIRE', KEYS[2], ARGV[2])
                    return 1
                    """, Long.class);
    private static final DefaultRedisScript<Long> REPLACE_MANUAL_SCRIPT =
            new DefaultRedisScript<>("""
                    local oldDraftId = redis.call('GET', KEYS[1])
                    if oldDraftId then
                        redis.call('DEL', ARGV[3] .. oldDraftId)
                        redis.call('DEL', ARGV[3] .. oldDraftId .. ARGV[4])
                        redis.call('ZREM', KEYS[3], oldDraftId)
                    end
                    redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[2])
                    redis.call('SET', KEYS[1], ARGV[5], 'PX', ARGV[2])
                    redis.call('ZADD', KEYS[3], ARGV[5], ARGV[5])
                    redis.call('PEXPIRE', KEYS[3], ARGV[2])
                    return 1
                    """, Long.class);
    private static final DefaultRedisScript<Long> DELETE_SCRIPT =
            new DefaultRedisScript<>("""
                    redis.call('DEL', KEYS[1], KEYS[2])
                    redis.call('ZREM', KEYS[3], ARGV[1])
                    if redis.call('GET', KEYS[4]) == ARGV[1] then
                        redis.call('DEL', KEYS[4])
                    end
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public TravelGuideDraftCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(TravelGuideDraft draft) {
        try {
            Long result = redisTemplate.execute(SAVE_SCRIPT,
                    List.of(key(draft.requestId()), userDraftIndexKey(draft.userId())),
                    OBJECT_MAPPER.writeValueAsString(draft), String.valueOf(TTL.toMillis()),
                    String.valueOf(draft.requestId()));
            requireSuccess(result);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    public void replaceManual(TravelGuideDraft draft) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(draft);
            Long result = redisTemplate.execute(REPLACE_MANUAL_SCRIPT,
                    List.of(manualPointerKey(draft.userId()), key(draft.requestId()),
                            userDraftIndexKey(draft.userId())),
                    json, String.valueOf(TTL.toMillis()), KEY_PREFIX, REFRESH_SUFFIX,
                    String.valueOf(draft.requestId()));
            requireSuccess(result);
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

    public List<TravelGuideDraft> findAllByUserId(Long userId) {
        try {
            String indexKey = userDraftIndexKey(userId);
            Set<String> draftIds = redisTemplate.opsForZSet()
                    .reverseRange(indexKey, 0, -1);
            if (draftIds == null || draftIds.isEmpty()) return List.of();

            List<TravelGuideDraft> drafts = new ArrayList<>();
            List<String> staleIds = new ArrayList<>();
            for (String value : draftIds) {
                try {
                    Long draftId = Long.valueOf(value);
                    Optional<TravelGuideDraft> draft = findOptional(draftId);
                    if (draft.isPresent() && draft.get().userId().equals(userId)) {
                        drafts.add(draft.get());
                    } else {
                        staleIds.add(value);
                    }
                } catch (NumberFormatException exception) {
                    staleIds.add(value);
                }
            }
            if (!staleIds.isEmpty()) {
                redisTemplate.opsForZSet().remove(indexKey, staleIds.toArray());
            }
            return List.copyOf(drafts);
        } catch (DataAccessException exception) {
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

    public void delete(Long draftId, Long userId) {
        try {
            Long result = redisTemplate.execute(DELETE_SCRIPT,
                    List.of(key(draftId), refreshKey(draftId), userDraftIndexKey(userId),
                            manualPointerKey(userId)),
                    String.valueOf(draftId));
            requireSuccess(result);
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

    private String userDraftIndexKey(Long userId) {
        return USER_DRAFT_INDEX_PREFIX + userId + USER_DRAFT_INDEX_SUFFIX;
    }

    private void requireSuccess(Long result) {
        if (!Long.valueOf(1L).equals(result)) throw unavailable();
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
