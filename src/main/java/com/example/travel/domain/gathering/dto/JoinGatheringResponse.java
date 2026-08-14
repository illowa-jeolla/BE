package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;

public record JoinGatheringResponse(
        Long gatheringId,
        ParticipantStatus participantStatus,
        long participantCount,
        GatheringStatus gatheringStatus
) {
}
