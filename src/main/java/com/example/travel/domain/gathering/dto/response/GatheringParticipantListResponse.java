package com.example.travel.domain.gathering.dto.response;

import com.example.travel.domain.gathering.dto.item.GatheringParticipantItem;

import java.util.List;

public record GatheringParticipantListResponse(
        Long gatheringId,
        long participantCount,
        List<GatheringParticipantItem> participants
) {
}
