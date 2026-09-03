package com.example.travel.domain.ai.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AiCandidateSyncServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 3);

    @Test
    void acceptsRecentJobWithoutDeadline() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY.minusDays(180), null, "관광 기획자 채용", TODAY)).isTrue();
    }

    @Test
    void rejectsOldJobWithoutDeadline() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY.minusDays(181), null, "관광 기획자 채용", TODAY)).isFalse();
    }

    @Test
    void acceptsOldJobWhenDeadlineHasNotPassed() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY.minusYears(1), TODAY.plusDays(1), "관광 기획자 채용", TODAY)).isTrue();
    }

    @Test
    void rejectsExpiredOrClosedJob() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY, TODAY.minusDays(1), "관광 기획자 채용", TODAY)).isFalse();
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY, TODAY.plusDays(1), "관광 기획자 채용마감", TODAY)).isFalse();
    }

    @Test
    void parsesSupportedExternalDateFormats() {
        assertThat(AiCandidateSyncService.date("20260903")).isEqualTo(TODAY);
        assertThat(AiCandidateSyncService.date("2026-09-03")).isEqualTo(TODAY);
        assertThat(AiCandidateSyncService.date("2026.09.03")).isEqualTo(TODAY);
        assertThat(AiCandidateSyncService.date("2026-09-03 13:20:10")).isEqualTo(TODAY);
    }
}
