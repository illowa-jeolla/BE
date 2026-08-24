package com.example.travel.domain.travel.service.cleanup;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class SavedTravelGuideCleanupScheduler {
    private final JobOperator jobOperator;
    private final Job savedTravelGuideCleanupJob;
    private final Clock clock;

    public SavedTravelGuideCleanupScheduler(JobOperator jobOperator,
                                            Job savedTravelGuideCleanupJob,
                                            Clock clock) {
        this.jobOperator = jobOperator;
        this.savedTravelGuideCleanupJob = savedTravelGuideCleanupJob;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${travel.saved-guide-cleanup.cron:0 0 5 * * MON}",
            zone = "${travel.saved-guide-cleanup.zone:Asia/Seoul}"
    )
    public void runSavedTravelGuideCleanupJob() throws Exception {
        jobOperator.start(
                savedTravelGuideCleanupJob,
                new JobParametersBuilder()
                        .addLong("launchedAt", clock.instant().toEpochMilli())
                        .toJobParameters()
        );
    }
}
