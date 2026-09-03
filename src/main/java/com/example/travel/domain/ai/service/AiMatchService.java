package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.dto.request.CreateAiMatchRequest;
import com.example.travel.domain.ai.dto.response.CreateAiMatchResponse;
import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.enums.PriorityType;
import com.example.travel.domain.ai.exception.AiMatchErrorCode;
import com.example.travel.domain.ai.exception.AiMatchException;
import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

@Service
public class AiMatchService {
    private final AiMatchRequestCacheService cacheService;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final AiMatchDailyLimitService dailyLimitService;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public AiMatchService(AiMatchRequestCacheService cacheService, RegionRepository regionRepository,
                          UserRepository userRepository, AiMatchDailyLimitService dailyLimitService,
                          ApplicationEventPublisher publisher, Clock clock) {
        this.cacheService = cacheService; this.regionRepository = regionRepository;
        this.userRepository = userRepository; this.dailyLimitService = dailyLimitService;
        this.publisher = publisher; this.clock = clock;
    }

    public CreateAiMatchResponse create(Long userId, CreateAiMatchRequest request) {
        if (new HashSet<>(request.priorities()).size() != PriorityType.values().length
                || !new HashSet<>(request.priorities()).containsAll(java.util.List.of(PriorityType.values()))) {
            throw new AiMatchException(AiMatchErrorCode.INVALID_PRIORITIES);
        }
        if (regionRepository.findActiveById(request.preferredRegionId()).isEmpty()) {
            throw new AiMatchException(AiMatchErrorCode.REGION_NOT_FOUND);
        }
        if (userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).isEmpty()) {
            throw new AiMatchException(AiMatchErrorCode.REQUEST_NOT_FOUND);
        }
        dailyLimitService.acquire(userId);
        UUID requestId = UUID.randomUUID();
        cacheService.save(new AiMatchRequestContext(requestId, userId, request.preferredRegionId(),
                request.desiredJobs(), request.priorities(), request.thought(), AiRequestStatus.PROCESSING,
                OffsetDateTime.now(clock), null));
        publisher.publishEvent(new AiMatchCreatedEvent(requestId));
        return new CreateAiMatchResponse(requestId, AiRequestStatus.PROCESSING);
    }
}
