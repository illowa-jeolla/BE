package com.example.travel.domain.community.service.cleanup;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class S3ObjectDeletionScheduler {
    private final S3ObjectDeletionProcessor processor;

    public S3ObjectDeletionScheduler(S3ObjectDeletionProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(cron = "${community.image-deletion.cron:0 */10 * * * *}",
            zone = "${community.image-deletion.zone:Asia/Seoul}")
    public void retry() {
        processor.processDue(OffsetDateTime.now());
    }
}
