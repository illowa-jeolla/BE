package com.example.travel.domain.community.service.cleanup;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeletedTravelPostCleanupSchedulerTest {
    @Test
    void usesSevenDayRetentionPeriod() {
        DeletedTravelPostCleanupService cleanupService =
                mock(DeletedTravelPostCleanupService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"),
                ZoneId.of("Asia/Seoul"));

        new DeletedTravelPostCleanupScheduler(cleanupService, clock).cleanup();

        verify(cleanupService).deleteExpired(
                OffsetDateTime.parse("2026-08-21T09:00:00+09:00"));
    }
}
