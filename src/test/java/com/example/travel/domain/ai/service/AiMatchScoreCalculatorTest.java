package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import com.example.travel.domain.ai.enums.PriorityType;
import com.example.travel.domain.ai.config.AiMatchScoreProperties;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiMatchScoreCalculatorTest {
    private final AiMatchScoreCalculator calculator = new AiMatchScoreCalculator(new AiMatchScoreProperties());

    @Test
    void calibratesSemanticSimilarityForUserFacingScores() {
        assertThat(calculator.semanticScore(0.14)).isZero();
        assertThat(calculator.semanticScore(0.30)).isEqualTo(70);
        assertThat(calculator.semanticScore(0.40)).isEqualTo(83);
        assertThat(calculator.semanticScore(0.50)).isEqualTo(92);
    }

    @Test
    void boostsJobsWhoseTitleMatchesDesiredJob() {
        AiJobCandidate matched = job("프로그래머 채용", "웹 서비스 개발");
        AiJobCandidate unmatched = job("관광 안내원 채용", "방문객 안내");

        assertThat(calculator.jobScore(0.30, matched, List.of("프로그래머")))
                .isGreaterThan(calculator.jobScore(0.30, unmatched, List.of("프로그래머")));
        assertThat(calculator.jobScore(0.30, matched, List.of("프로그래머"))).isEqualTo(76);
    }

    @Test
    void combinesPlaceFitAvailabilityAndDiversity() {
        assertThat(calculator.tourismScore(72, 20, 4)).isEqualTo(80);
        assertThat(calculator.tourismScore(0, 20, 4)).isZero();
    }

    @Test
    void includesPreferredRegionInOverallScore() {
        int overall = calculator.overall(
                List.of(PriorityType.JOB, PriorityType.HOUSING,
                        PriorityType.TOURISM, PriorityType.COMMUNITY),
                100, 70, 75, 80, 70);

        assertThat(overall).isEqualTo(78);
    }

    private AiJobCandidate job(String title, String description) {
        return AiJobCandidate.create(ExternalCandidateSource.TOUR_JOB, "job-1", null,
                title, "회사", "전라남도 여수시", description, "정규직", null,
                null, null, null, OffsetDateTime.parse("2026-09-21T00:00:00Z"));
    }
}
