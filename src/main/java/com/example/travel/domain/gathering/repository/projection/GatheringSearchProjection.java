package com.example.travel.domain.gathering.repository.projection;

import com.example.travel.domain.gathering.enums.GatheringStatus;

import java.time.OffsetDateTime;

public record GatheringSearchProjection(
        Long id,
        String title,
        Long regionId,
        String regionName,
        String concept,
        String meetingPlace,
        OffsetDateTime startsAt,
        short capacity,
        GatheringStatus status,
        Long creatorId,
        String creatorNickname,
        Long participantCount,
        Long joinedCount
) {
}
