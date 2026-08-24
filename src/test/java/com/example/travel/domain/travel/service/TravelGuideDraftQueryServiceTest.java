package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TravelGuideDraftQueryServiceTest {
    @Test
    void returnsDraftSummariesForNavigationToDetails() {
        TravelGuideDraftCacheService cacheService = mock(TravelGuideDraftCacheService.class);
        TravelRecommendationRequest request = mock(TravelRecommendationRequest.class);
        when(request.getRegionName()).thenReturn("여수");
        when(request.getStartsOn()).thenReturn(LocalDate.of(2026, 9, 1));
        when(request.getEndsOn()).thenReturn(LocalDate.of(2026, 9, 2));
        AiTravelGuideResult result = new AiTravelGuideResult(
                "여수 바다 여행", "요약", List.of(), "팁");
        TravelGuideDraft draft = new TravelGuideDraft(
                42L, 1L, request, result, List.of(), List.of(), true, false);
        when(cacheService.findAllByUserId(1L)).thenReturn(List.of(draft));

        var summaries = new TravelGuideDraftQueryService(cacheService).findAll(1L);

        assertThat(summaries).singleElement().satisfies(summary -> {
            assertThat(summary.draftId()).isEqualTo(42L);
            assertThat(summary.title()).isEqualTo("여수 바다 여행");
            assertThat(summary.regionName()).isEqualTo("여수");
            assertThat(summary.startsOn()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(summary.endsOn()).isEqualTo(LocalDate.of(2026, 9, 2));
            assertThat(summary.generatedByAi()).isTrue();
        });
    }
}
