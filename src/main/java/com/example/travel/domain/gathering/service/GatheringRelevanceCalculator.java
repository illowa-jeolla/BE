package com.example.travel.domain.gathering.service;

import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class GatheringRelevanceCalculator {
    public double timeScore(LocalTime requested, LocalTime actual) {
        if (requested == null) {
            return 0.0;
        }
        int difference = Math.abs(requested.toSecondOfDay() - actual.toSecondOfDay());
        int circularDifference = Math.min(difference, 86_400 - difference);
        return round(1.0 - ((double) circularDifference / 43_200));
    }

    public double conceptScore(String requested, String actual) {
        return textScore(requested, actual);
    }

    public double meetingPlaceScore(String requested, String actual) {
        return textScore(requested, actual);
    }

    private double textScore(String requested, String actual) {
        String expected = normalize(requested);
        String candidate = normalize(actual);
        if (expected == null || candidate == null) {
            return 0.0;
        }
        if (expected.equals(candidate)) {
            return 1.0;
        }

        double diceScore = diceCoefficient(expected, candidate);
        if (expected.contains(candidate) || candidate.contains(expected)) {
            diceScore = Math.max(diceScore, 0.8);
        }
        return round(diceScore);
    }

    public double relevanceScore(Double... scores) {
        double sum = 0.0;
        int count = 0;
        for (Double score : scores) {
            if (score != null) {
                sum += score;
                count++;
            }
        }
        return count == 0 ? 0.0 : round(sum / count);
    }

    private double diceCoefficient(String left, String right) {
        Set<String> leftBigrams = bigrams(left);
        Set<String> rightBigrams = bigrams(right);
        if (leftBigrams.isEmpty() || rightBigrams.isEmpty()) {
            return 0.0;
        }
        long intersection = leftBigrams.stream().filter(rightBigrams::contains).count();
        return (2.0 * intersection) / (leftBigrams.size() + rightBigrams.size());
    }

    private Set<String> bigrams(String value) {
        Set<String> result = new HashSet<>();
        for (int index = 0; index < value.length() - 1; index++) {
            result.add(value.substring(index, index + 2));
        }
        return result;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
