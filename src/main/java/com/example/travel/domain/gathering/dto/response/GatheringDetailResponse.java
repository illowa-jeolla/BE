package com.example.travel.domain.gathering.dto.response;

import com.example.travel.domain.gathering.enums.GatheringStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GatheringDetailResponse(
        Long id,
        String title,
        String description,
        RegionSummary region,
        String concept,
        String meetingPlace,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime startsAt,
        short capacity,
        long participantCount,
        GatheringStatus status,
        boolean joined,
        boolean host,
        CreatorSummary creator,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record RegionSummary(Long id, String name) {
    }

    public record CreatorSummary(Long id, String nickname, String avatarUrl) {
    }
}
