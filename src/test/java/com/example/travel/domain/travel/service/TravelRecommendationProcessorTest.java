package com.example.travel.domain.travel.service;

import com.example.travel.domain.ai.exception.OpenAiErrorCode;
import com.example.travel.domain.ai.exception.OpenAiException;
import com.example.travel.domain.travel.ai.TravelAiService;
import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import com.example.travel.domain.travel.route.PlannedRouteSegment;
import com.example.travel.domain.travel.route.TravelRouteService;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;

@ExtendWith(MockitoExtension.class)
class TravelRecommendationProcessorTest {
    @Mock private TravelRecommendationRequestCacheService requestCacheService;
    @Mock private TravelCandidateCacheService candidateCacheService;
    @Mock private TravelAiService travelAiService;
    @Mock private TravelFallbackService fallbackService;
    @Mock private TravelRouteService routeService;
    @Mock private TravelGuideDraftCacheService draftCacheService;
    @Mock private TravelRecommendationRequest request;
    @Mock private AiTravelGuideResult aiResult;
    @Mock private AiTravelGuideResult fallbackResult;

    @InjectMocks private TravelRecommendationProcessor processor;

    @Test
    void savesAiResultAndDeletesCache() {
        List<TravelCandidateItem> candidates = List.of();
        when(requestCacheService.find(10L)).thenReturn(request);
        when(request.getUserId()).thenReturn(1L);
        when(candidateCacheService.find(10L)).thenReturn(candidates);
        when(travelAiService.generate(request, candidates)).thenReturn(aiResult);
        when(routeService.plan(request, aiResult, candidates)).thenReturn(List.of());

        processor.process(10L);

        verify(draftCacheService).save(any(TravelGuideDraft.class));
        verify(request).markCompleted();
        verify(requestCacheService, atLeast(2)).save(request);
        verify(candidateCacheService).delete(10L);
    }

    @Test
    void savesFallbackWhenOpenAiFails() {
        List<TravelCandidateItem> candidates = List.of();
        when(requestCacheService.find(10L)).thenReturn(request);
        when(request.getUserId()).thenReturn(1L);
        when(candidateCacheService.find(10L)).thenReturn(candidates);
        when(travelAiService.generate(request, candidates))
                .thenThrow(new OpenAiException(OpenAiErrorCode.TIMEOUT));
        when(fallbackService.create(request, candidates)).thenReturn(fallbackResult);
        when(routeService.plan(request, fallbackResult, candidates)).thenReturn(List.of());

        processor.process(10L);

        verify(draftCacheService).save(any(TravelGuideDraft.class));
        verify(request).markCompleted();
    }

    @Test
    void marksRequestFailedWhenCandidatesCannotBeLoaded() {
        when(requestCacheService.find(10L)).thenReturn(request);
        when(candidateCacheService.find(10L)).thenThrow(new RuntimeException());

        processor.process(10L);

        verify(request).markFailed();
        verify(draftCacheService, never()).save(any());
        verify(request, never()).markCompleted();
    }
}
