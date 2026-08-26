package com.example.travel.domain.community.service.cleanup;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class TravelPostDraftCleanupScheduler {
    private final TravelPostDraftCleanupService cleanupService;
    private final Clock clock;

    public TravelPostDraftCleanupScheduler(TravelPostDraftCleanupService cleanupService,
                                           Clock clock) {
        this.cleanupService = cleanupService;
        this.clock = clock;
    }

    @Scheduled(cron = "${community.draft-cleanup.cron:0 0 4 * * *}",
            zone = "${community.draft-cleanup.zone:Asia/Seoul}")
    public void cleanup() {
        cleanupService.deleteExpired(OffsetDateTime.now(clock).minusDays(7));
    }
}
