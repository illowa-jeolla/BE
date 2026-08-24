package com.example.travel.domain.travel.config;

import com.example.travel.domain.travel.service.cleanup.SavedTravelGuideCleanupService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TravelBatchConfig {
    @Bean
    Job savedTravelGuideCleanupJob(JobRepository jobRepository,
                                   Step savedTravelGuideCleanupStep) {
        return new JobBuilder("savedTravelGuideCleanupJob", jobRepository)
                .start(savedTravelGuideCleanupStep)
                .build();
    }

    @Bean
    Step savedTravelGuideCleanupStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     SavedTravelGuideCleanupService cleanupService) {
        return new StepBuilder("savedTravelGuideCleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    cleanupService.deleteSoftDeleted();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
