package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.GatheringTiming;

import java.time.OffsetDateTime;

public record MyGatheringItem(
        Long id,
        String title,
        RegionSummary region,
        String concept,
        String meetingPlace,
        OffsetDateTime startsAt,
        short capacity,
        long participantCount,
        GatheringStatus status,
        GatheringTiming timing
) {
    public record RegionSummary(Long id, String name) {
    }
}
