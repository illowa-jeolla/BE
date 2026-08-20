package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.CompanionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelGuideDraftCacheServiceTest {
    @Test
    void storesAndReadsCompleteDraftAsJsonForTwentyFourHours() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        TravelGuideDraftCacheService service = new TravelGuideDraftCacheService(redisTemplate);
        AiTravelGuideResult result = new AiTravelGuideResult("제목", "요약", List.of(
                new AiTravelGuideResult.Day(1, "1일차", List.of(
                        new AiTravelGuideResult.Item("100", 1, "10:00", 60, "추천")))), "팁");
        TravelCandidateItem candidate = new TravelCandidateItem("100", "관광지", "주소",
                null, new BigDecimal("34.1"), new BigDecimal("126.1"), 1000, 80);
        TravelRecommendationRequest request = TravelRecommendationRequest.create(10L, 1L, 9L,
                "완도", "lodging", "숙소", "주소", new BigDecimal("34.1"),
                new BigDecimal("126.1"), "start", "출발", "주소",
                new BigDecimal("34.1"), new BigDecimal("126.1"), "end", "도착", "주소",
                new BigDecimal("34.2"), new BigDecimal("126.2"),
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 20),
                new String[]{"NATURE_HEALING"}, new Integer[]{1},
                TransportType.CAR, CompanionType.COUPLE);
        TravelGuideDraft draft = new TravelGuideDraft(10L, 1L, request, result,
                List.of(candidate), List.of(), true, false);

        service.save(draft);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(values).set(org.mockito.ArgumentMatchers.eq("travel:guide:draft:10"),
                json.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofHours(24)));
        when(values.get("travel:guide:draft:10")).thenReturn(json.getValue());
        TravelGuideDraft restored = service.find(10L);
        assertThat(restored.requestId()).isEqualTo(10L);
        assertThat(restored.request().getRegionName()).isEqualTo("완도");
        assertThat(restored.request().getStartsOn()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(restored.result()).isEqualTo(result);
        assertThat(restored.candidates()).containsExactly(candidate);
    }
}
