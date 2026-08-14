package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.GatheringStatus;

import java.time.OffsetDateTime;

public record UpdateGatheringResponse(
        Long id,
        String title,
        String description,
        String concept,
        String meetingPlace,
        OffsetDateTime startsAt,
        short capacity,
        long participantCount,
        GatheringStatus status
) {
}
