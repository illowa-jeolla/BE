package com.example.travel.domain.gathering.repository.projection;

import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;

import java.time.OffsetDateTime;

public record GatheringParticipantProjection(
        Long userId,
        String nickname,
        String avatarUrl,
        ParticipantRole role,
        ParticipantStatus status,
        OffsetDateTime joinedAt
) {
}
