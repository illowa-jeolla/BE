package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.service.cleanup.ExpiredGatheringScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpiredGatheringSchedulerTest {
    @Test
    void launchesCleanupBatchJobWithUniqueLaunchTime() throws Exception {
        JobOperator jobOperator = mock(JobOperator.class);
        Job job = mock(Job.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC);
        ExpiredGatheringScheduler scheduler =
                new ExpiredGatheringScheduler(jobOperator, job, clock);

        scheduler.runExpiredGatheringCleanupJob();

        JobParameters expected = new JobParametersBuilder()
                .addLong("launchedAt", clock.instant().toEpochMilli())
                .toJobParameters();
        verify(jobOperator).start(eq(job), eq(expected));
    }
}
