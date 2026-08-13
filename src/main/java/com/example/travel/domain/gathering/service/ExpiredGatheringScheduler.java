package com.example.travel.domain.gathering.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class ExpiredGatheringScheduler {
    private final ExpiredGatheringCleanupService cleanupService;
    private final Clock clock;

    public ExpiredGatheringScheduler(ExpiredGatheringCleanupService cleanupService, Clock clock) {
        this.cleanupService = cleanupService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${gathering.cleanup.fixed-delay:60000}")
    public void deleteExpiredGatherings() {
        cleanupService.deleteExpired(OffsetDateTime.now(clock));
    }
}
