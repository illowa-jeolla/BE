package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.ai.TravelAiService;
import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.example.travel.domain.travel.route.PlannedRouteSegment;
import com.example.travel.domain.travel.route.TravelRouteService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
public class TravelRecommendationProcessor {
    private final TravelCandidateCacheService candidateCacheService;
    private final TravelAiService travelAiService;
    private final TravelFallbackService fallbackService;
    private final TravelRouteService routeService;
    private final TravelGuideDraftCacheService draftCacheService;
    private final TravelRecommendationRequestCacheService requestCacheService;

    public TravelRecommendationProcessor(
            TravelCandidateCacheService candidateCacheService,
            TravelAiService travelAiService,
            TravelFallbackService fallbackService,
            TravelRouteService routeService,
            TravelGuideDraftCacheService draftCacheService,
            TravelRecommendationRequestCacheService requestCacheService) {
        this.candidateCacheService = candidateCacheService;
        this.travelAiService = travelAiService;
        this.fallbackService = fallbackService;
        this.routeService = routeService;
        this.draftCacheService = draftCacheService;
        this.requestCacheService = requestCacheService;
    }

    public void process(Long requestId) {
        try {
            TravelRecommendationContext request = requestCacheService.find(requestId);
            request.markProcessing();
            requestCacheService.save(request);
            List<TravelCandidateItem> candidates = candidateCacheService.find(requestId);
            AiTravelGuideResult result;
            boolean generatedByAi;
            try {
                result = travelAiService.generate(request, candidates);
                generatedByAi = true;
            } catch (RuntimeException exception) {
                log.warn("OpenAI travel recommendation failed; using fallback. requestId={}",
                        requestId, exception);
                result = fallbackService.create(request, candidates);
                generatedByAi = false;
            }
            List<PlannedRouteSegment> routes = routeService.plan(request, result, candidates);
            draftCacheService.save(new TravelGuideDraft(requestId, request.getUserId(), request,
                    result, candidates, routes, generatedByAi, request.isRefreshRequest()));
            request.markCompleted();
            requestCacheService.save(request);
            deleteCacheQuietly(requestId);
        } catch (RuntimeException exception) {
            log.error("Travel recommendation processing failed. requestId={}",
                    requestId, exception);
            try {
                TravelRecommendationContext request = requestCacheService.find(requestId);
                request.markFailed();
                requestCacheService.save(request);
            } catch (RuntimeException ignored) {
                // 요청 컨텍스트가 만료되었거나 Redis를 사용할 수 없으면 추가 상태 기록도 불가능하다.
            }
        }
    }

    private void deleteCacheQuietly(Long requestId) {
        try {
            candidateCacheService.delete(requestId);
        } catch (RuntimeException ignored) {
            // TTL이 만료되면 자동 정리되므로 완료 결과는 유지한다.
        }
    }
}
