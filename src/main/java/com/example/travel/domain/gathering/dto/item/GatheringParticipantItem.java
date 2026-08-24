package com.example.travel.domain.gathering.dto.item;

import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;

import java.time.OffsetDateTime;

public record GatheringParticipantItem(
        Long userId,
        String nickname,
        String avatarUrl,
        ParticipantRole role,
        ParticipantStatus status,
        OffsetDateTime joinedAt
) {
}
