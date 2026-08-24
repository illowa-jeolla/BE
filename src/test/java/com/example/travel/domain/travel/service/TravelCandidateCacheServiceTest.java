package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TravelCandidateCacheServiceTest {
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private TravelCandidateCacheService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new TravelCandidateCacheService(redisTemplate);
    }

    @Test
    void savesCandidatesForOneHour() {
        List<TravelCandidateItem> candidates = List.of(candidate());

        service.save(10L, candidates);

        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("travel:recommendation:10:candidates"),
                anyString(), org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)));
    }

    @Test
    void readsSavedCandidates() throws Exception {
        String json = new ObjectMapper().writeValueAsString(List.of(candidate()));
        when(valueOperations.get("travel:recommendation:10:candidates")).thenReturn(json);

        List<TravelCandidateItem> result = service.find(10L);

        assertThat(result).containsExactly(candidate());
    }

    @Test
    void reportsExpiredCandidates() {
        when(valueOperations.get("travel:recommendation:10:candidates")).thenReturn(null);

        assertThatThrownBy(() -> service.find(10L))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.CANDIDATE_CACHE_NOT_FOUND.code());
    }

    @Test
    void deletesCandidateCache() {
        service.delete(10L);

        verify(redisTemplate).delete("travel:recommendation:10:candidates");
    }

    private TravelCandidateItem candidate() {
        return new TravelCandidateItem("100", "완도타워", "전남 완도군", null,
                new BigDecimal("34.31"), new BigDecimal("126.75"), 1000, 80);
    }
}
