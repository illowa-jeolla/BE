package com.example.travel.domain.community.service.cleanup;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class DeletedTravelPostCleanupScheduler {
    private static final long RETENTION_DAYS = 7;

    private final DeletedTravelPostCleanupService cleanupService;
    private final Clock clock;

    public DeletedTravelPostCleanupScheduler(DeletedTravelPostCleanupService cleanupService,
                                             Clock clock) {
        this.cleanupService = cleanupService;
        this.clock = clock;
    }

    @Scheduled(cron = "${community.deleted-post-cleanup.cron:0 30 4 * * *}",
            zone = "${community.deleted-post-cleanup.zone:Asia/Seoul}")
    public void cleanup() {
        cleanupService.deleteExpired(OffsetDateTime.now(clock).minusDays(RETENTION_DAYS));
    }
}
