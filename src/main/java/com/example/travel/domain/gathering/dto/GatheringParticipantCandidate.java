package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;

import java.time.OffsetDateTime;

public record GatheringParticipantCandidate(
        Long userId,
        String nickname,
        String avatarUrl,
        ParticipantRole role,
        ParticipantStatus status,
        OffsetDateTime joinedAt
) {
}
