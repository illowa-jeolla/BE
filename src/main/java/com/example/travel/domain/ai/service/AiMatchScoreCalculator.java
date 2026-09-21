package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.enums.PriorityType;
import com.example.travel.domain.ai.config.AiMatchScoreProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AiMatchScoreCalculator {
    private static final Set<String> IGNORED_JOB_TOKENS = Set.of(
            "전체", "기타", "직무", "업무", "관련", "분야");
    private final double minimumSimilarity;
    private final double scoreCenter;
    private final double scoreScale;
    private final int scoreFloor;
    private final int scoreRange;

    public AiMatchScoreCalculator(AiMatchScoreProperties properties) {
        if (properties.getMinimumSimilarity() < 0 || properties.getMinimumSimilarity() > 1
                || properties.getScale() <= 0 || properties.getFloor() < 0
                || properties.getFloor() > 100 || properties.getRange() < 0) {
            throw new IllegalArgumentException("Invalid AI match score configuration");
        }
        this.minimumSimilarity = properties.getMinimumSimilarity();
        this.scoreCenter = properties.getCenter();
        this.scoreScale = properties.getScale();
        this.scoreFloor = properties.getFloor();
        this.scoreRange = properties.getRange();
    }

    public boolean isEligible(double similarity) {
        return similarity >= minimumSimilarity;
    }

    public int semanticScore(double similarity) {
        if (!isEligible(similarity)) return 0;
        double normalized = 1.0 / (1.0 + Math.exp(-(similarity - scoreCenter) / scoreScale));
        return clamp((int) Math.round(scoreFloor + scoreRange * normalized), 0, 98);
    }

    public int jobScore(double similarity, AiJobCandidate job, List<String> desiredJobs) {
        int semantic = semanticScore(similarity);
        if (semantic == 0) return 0;
        int keyword = keywordScore(job, desiredJobs);
        return clamp((int) Math.round(semantic * 0.8 + keyword * 0.2), 0, 98);
    }

    public int tourismScore(int selectedPlaceAverage, long availablePlaceCount,
                            long categoryDiversity) {
        if (selectedPlaceAverage == 0) return 0;
        int availability = clamp((int) (55 + availablePlaceCount * 2), 55, 100);
        int diversity = clamp((int) (50 + categoryDiversity * 10), 50, 100);
        return clamp((int) Math.round(selectedPlaceAverage * 0.60
                + availability * 0.25 + diversity * 0.15), 0, 98);
    }

    public int overall(List<PriorityType> priorities, int region, int job,
                       int tourism, int housing, int community) {
        int[] weights = {40, 30, 20, 10};
        Map<PriorityType, Integer> values = Map.of(
                PriorityType.JOB, job,
                PriorityType.TOURISM, tourism,
                PriorityType.HOUSING, housing,
                PriorityType.COMMUNITY, community);
        int weighted = 0;
        for (int i = 0; i < priorities.size(); i++) {
            weighted += values.get(priorities.get(i)) * weights[i];
        }
        int priorityScore = Math.round(weighted / 100f);
        return clamp(Math.round(priorityScore * 0.85f + region * 0.15f), 0, 100);
    }

    private int keywordScore(AiJobCandidate job, List<String> desiredJobs) {
        String title = normalize(job.getTitle());
        String details = normalize(String.join(" ", text(job.getTitle()),
                text(job.getJobDescription()), text(job.getEmploymentType())));
        for (String desiredJob : desiredJobs) {
            String normalizedDesired = normalize(desiredJob);
            if (!normalizedDesired.isBlank() && title.contains(normalizedDesired)) return 100;
        }
        List<String> tokens = desiredJobs.stream()
                .flatMap(value -> Arrays.stream(text(value).split("[\\s·,/()_\\-]+")))
                .map(this::normalize)
                .filter(value -> value.length() >= 2 && !IGNORED_JOB_TOKENS.contains(value))
                .distinct()
                .toList();
        if (tokens.stream().anyMatch(title::contains)) return 95;
        if (desiredJobs.stream().map(this::normalize)
                .anyMatch(value -> !value.isBlank() && details.contains(value))) return 90;
        if (tokens.stream().anyMatch(details::contains)) return 85;
        return 65;
    }

    private String normalize(String value) {
        return text(value).toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]", "");
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.min(maximum, Math.max(minimum, value));
    }
}
