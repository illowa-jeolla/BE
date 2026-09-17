package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashSet;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.CompanionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class TravelGuideDraftCacheServiceTest {
    @Test
    void storesAndReadsCompleteDraftAsJsonForTwentyFourHours() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(redisTemplate.execute(any(RedisScript.class), any(), any(Object[].class)))
                .thenReturn(1L);
        TravelGuideDraftCacheService service = new TravelGuideDraftCacheService(redisTemplate);
        TravelGuideDraft draft = draft();

        service.save(draft);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).execute(any(), eq(List.of(
                        "travel:guide:draft:10", "travel:guide:user:1:drafts")),
                json.capture(), eq(String.valueOf(Duration.ofHours(24).toMillis())), eq("10"));
        when(values.get("travel:guide:draft:10")).thenReturn(json.getValue());
        TravelGuideDraft restored = service.find(10L);
        assertThat(restored.requestId()).isEqualTo(10L);
        assertThat(restored.request().getRegionName()).isEqualTo("완도");
        assertThat(restored.request().getStartsOn()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(restored.result()).isEqualTo(draft.result());
        assertThat(restored.candidates()).containsExactlyElementsOf(draft.candidates());
    }

    @Test
    void atomicallyReplacesTheUsersPreviousManualDraft() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        TravelGuideDraftCacheService service = new TravelGuideDraftCacheService(redisTemplate);
        when(redisTemplate.execute(any(RedisScript.class), any(), any(Object[].class)))
                .thenReturn(1L);

        service.replaceManual(draft());

        verify(redisTemplate).execute(any(), eq(List.of(
                        "travel:guide:manual:user:1:draft-id", "travel:guide:draft:10",
                        "travel:guide:user:1:drafts")),
                any(), eq(String.valueOf(Duration.ofHours(24).toMillis())),
                eq("travel:guide:draft:"), eq(":refresh-used"), eq("10"));
    }

    @Test
    void returnsUsersDraftsNewestFirstAndRemovesStaleIndexEntries() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> sortedSets = mock(ZSetOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(redisTemplate.opsForZSet()).thenReturn(sortedSets);
        when(redisTemplate.execute(any(RedisScript.class), any(), any(Object[].class)))
                .thenReturn(1L);
        TravelGuideDraftCacheService service = new TravelGuideDraftCacheService(redisTemplate);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        service.save(draft());
        verify(redisTemplate).execute(any(), eq(List.of(
                        "travel:guide:draft:10", "travel:guide:user:1:drafts")),
                json.capture(), any(), any());
        when(sortedSets.reverseRange("travel:guide:user:1:drafts", 0, -1))
                .thenReturn(new LinkedHashSet<>(List.of("10", "999", "invalid")));
        when(values.get("travel:guide:draft:10")).thenReturn(json.getValue());

        List<TravelGuideDraft> result = service.findAllByUserId(1L);

        assertThat(result).extracting(TravelGuideDraft::requestId).containsExactly(10L);
        verify(sortedSets).remove("travel:guide:user:1:drafts", "999", "invalid");
    }

    @Test
    void rejectsAnUnconfirmedManualDraftReplacement() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        TravelGuideDraftCacheService service = new TravelGuideDraftCacheService(redisTemplate);

        assertThatThrownBy(() -> service.replaceManual(draft()))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.CANDIDATE_CACHE_UNAVAILABLE.code());
    }

    private TravelGuideDraft draft() {
        AiTravelGuideResult result = new AiTravelGuideResult("제목", "요약", List.of(
                new AiTravelGuideResult.Day(1, "1일차", List.of(
                        new AiTravelGuideResult.Item("100", 1, "10:00", 60, "추천")))), "팁");
        TravelCandidateItem candidate = new TravelCandidateItem("100", "관광지", "주소",
                null, new BigDecimal("34.1"), new BigDecimal("126.1"), 1000, 80);
        TravelRecommendationContext request = TravelRecommendationContext.create(10L, 1L, 9L,
                "완도", "lodging", "숙소", "주소", new BigDecimal("34.1"),
                new BigDecimal("126.1"), "start", "출발", "주소",
                new BigDecimal("34.1"), new BigDecimal("126.1"), "end", "도착", "주소",
                new BigDecimal("34.2"), new BigDecimal("126.2"),
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 20),
                new String[]{"NATURE_HEALING"}, new Integer[]{1},
                TransportType.CAR, CompanionType.COUPLE);
        TravelGuideDraft draft = new TravelGuideDraft(10L, 1L, request, result,
                List.of(candidate), List.of(), true, false);
        return draft;
    }
}
