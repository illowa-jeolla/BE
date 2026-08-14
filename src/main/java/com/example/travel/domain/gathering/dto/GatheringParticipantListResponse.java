package com.example.travel.domain.gathering.dto;

import java.util.List;

public record GatheringParticipantListResponse(
        Long gatheringId,
        long participantCount,
        List<GatheringParticipantItem> participants
) {
}
