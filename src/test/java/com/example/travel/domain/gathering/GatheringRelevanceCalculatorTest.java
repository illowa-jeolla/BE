package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.service.GatheringRelevanceCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class GatheringRelevanceCalculatorTest {
    private final GatheringRelevanceCalculator calculator = new GatheringRelevanceCalculator();

    @Test
    void exactTimeAndConceptReceiveHighestScore() {
        assertThat(calculator.timeScore(LocalTime.of(19, 0), LocalTime.of(19, 0)))
                .isEqualTo(1.0);
        assertThat(calculator.conceptScore("펍투어", "펍투어")).isEqualTo(1.0);
    }

    @Test
    void closerTimeReceivesHigherScoreWithoutExcludingDistantTime() {
        double close = calculator.timeScore(LocalTime.of(19, 0), LocalTime.of(18, 30));
        double distant = calculator.timeScore(LocalTime.of(19, 0), LocalTime.of(10, 0));

        assertThat(close).isGreaterThan(distant);
        assertThat(distant).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void partialConceptReceivesHigherScoreThanUnrelatedConcept() {
        double partial = calculator.conceptScore("펍투어", "여수 야간 펍투어");
        double unrelated = calculator.conceptScore("펍투어", "해변 산책");

        assertThat(partial).isGreaterThan(unrelated);
    }

    @Test
    void partialMeetingPlaceReceivesHigherScoreThanUnrelatedPlace() {
        double partial = calculator.meetingPlaceScore(
                "낭만포차", "여수 낭만포차 입구");
        double unrelated = calculator.meetingPlaceScore(
                "낭만포차", "오동도 주차장");

        assertThat(partial).isGreaterThan(unrelated);
    }
}
