package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.GatheringStatus;

import java.time.OffsetDateTime;

public record MyGatheringCandidate(
        Long id,
        String title,
        Long regionId,
        String regionName,
        String concept,
        String meetingPlace,
        OffsetDateTime startsAt,
        short capacity,
        GatheringStatus status,
        long participantCount
) {
}
