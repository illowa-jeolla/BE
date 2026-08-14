package com.example.travel.domain.gathering.service;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class ExpiredGatheringScheduler {
    private final JobOperator jobOperator;
    private final Job expiredGatheringCleanupJob;
    private final Clock clock;

    public ExpiredGatheringScheduler(JobOperator jobOperator,
                                     Job expiredGatheringCleanupJob,
                                     Clock clock) {
        this.jobOperator = jobOperator;
        this.expiredGatheringCleanupJob = expiredGatheringCleanupJob;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${gathering.cleanup.cron:0 0 0 * * MON}",
            zone = "${gathering.cleanup.zone:Asia/Seoul}"
    )
    public void runExpiredGatheringCleanupJob() throws Exception {
        Instant launchedAt = clock.instant();
        jobOperator.start(
                expiredGatheringCleanupJob,
                new JobParametersBuilder()
                        .addLong("launchedAt", launchedAt.toEpochMilli())
                        .toJobParameters()
        );
    }
}
