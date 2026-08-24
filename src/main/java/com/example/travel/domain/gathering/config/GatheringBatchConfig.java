package com.example.travel.domain.gathering.config;

import com.example.travel.domain.gathering.service.cleanup.ExpiredGatheringCleanupService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.OffsetDateTime;

@Configuration
public class GatheringBatchConfig {
    @Bean
    Job expiredGatheringCleanupJob(JobRepository jobRepository,
                                    Step expiredGatheringCleanupStep) {
        return new JobBuilder("expiredGatheringCleanupJob", jobRepository)
                .start(expiredGatheringCleanupStep)
                .build();
    }

    @Bean
    Step expiredGatheringCleanupStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      ExpiredGatheringCleanupService cleanupService,
                                      Clock clock) {
        return new StepBuilder("expiredGatheringCleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    cleanupService.deleteExpired(OffsetDateTime.now(clock));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
