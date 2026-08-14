package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.GatheringStatus;

import java.time.OffsetDateTime;

public record GatheringSearchItem(
        Long id,
        String title,
        RegionSummary region,
        String concept,
        String meetingPlace,
        OffsetDateTime startsAt,
        short capacity,
        long participantCount,
        GatheringStatus status,
        boolean joined,
        CreatorSummary creator,
        double timeScore,
        double conceptScore,
        double meetingPlaceScore,
        double relevanceScore
) {
    public record RegionSummary(Long id, String name) {
    }

    public record CreatorSummary(Long id, String nickname) {
    }
}
