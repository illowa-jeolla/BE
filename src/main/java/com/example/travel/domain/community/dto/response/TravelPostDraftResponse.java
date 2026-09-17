package com.example.travel.domain.community.dto.response;

import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.enums.TravelPostStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record TravelPostDraftResponse(
        Long draftId,
        TravelPostStatus status,
        Long regionId,
        String regionName,
        String title,
        String concept,
        String content,
        OffsetDateTime updatedAt,
        List<DraftImageItem> images
) {
    public static TravelPostDraftResponse from(TravelPost draft, List<DraftImageItem> images) {
        return new TravelPostDraftResponse(
                draft.getId(),
                draft.getStatus(),
                draft.getRegion() == null ? null : draft.getRegion().getId(),
                draft.getRegion() == null ? null : draft.getRegion().getName(),
                draft.getTitle(),
                draft.getConcept(),
                draft.getContent(),
                draft.getUpdatedAt(),
                images);
    }
}
