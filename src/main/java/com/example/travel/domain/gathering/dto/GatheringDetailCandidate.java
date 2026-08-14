package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.GatheringStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GatheringDetailCandidate(
        Long id,
        String title,
        String description,
        Long regionId,
        String regionName,
        String concept,
        String meetingPlace,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime startsAt,
        short capacity,
        GatheringStatus status,
        Long creatorId,
        String creatorNickname,
        String creatorAvatarUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long participantCount,
        long joinedCount
) {
}
