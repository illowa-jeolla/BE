package com.example.travel.domain.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiCandidateSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(AiCandidateSyncScheduler.class);

    private final AiCandidateSyncService service;
    public AiCandidateSyncScheduler(AiCandidateSyncService service) { this.service = service; }

    @Scheduled(cron = "${ai-match.sync.tour-place-cron}", zone = "${ai-match.sync.zone}")
    public void syncTourPlaces() {
        log.info("AI 관광지 후보 배치를 시작했습니다.");
        service.syncTourPlaces();
        log.info("AI 관광지 후보 배치를 종료했습니다.");
    }

    @Scheduled(cron = "${ai-match.sync.junnam-job-cron}", zone = "${ai-match.sync.zone}")
    public void syncJunnamJobs() {
        log.info("AI 전남 일자리 후보 배치를 시작했습니다.");
        service.syncJunnamJobs();
        log.info("AI 전남 일자리 후보 배치를 종료했습니다.");
    }

    @Scheduled(cron = "${ai-match.sync.tour-job-cron}", zone = "${ai-match.sync.zone}")
    public void syncTourJobs() {
        log.info("AI 전남·광주 관광 일자리 후보 배치를 시작했습니다.");
        service.syncTourJobs();
        log.info("AI 전남·광주 관광 일자리 후보 배치를 종료했습니다.");
    }

    @Scheduled(cron = "${ai-match.sync.cleanup-cron}", zone = "${ai-match.sync.zone}")
    public void cleanupInactive() {
        log.info("AI 비활성 후보 정리 배치를 시작했습니다.");
        service.cleanupInactive();
        log.info("AI 비활성 후보 정리 배치를 종료했습니다.");
    }
}
