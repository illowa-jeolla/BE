package com.example.travel.domain.community.dto.response;

import com.example.travel.domain.community.enums.TravelPostStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateTravelPostDraftResponse(
        Long draftId,
        TravelPostStatus status,
        Long regionId,
        String regionName,
        String title,
        String concept,
        String content,
        OffsetDateTime updatedAt,
        List<DraftImageItem> images,
        boolean resumed
) {
}
