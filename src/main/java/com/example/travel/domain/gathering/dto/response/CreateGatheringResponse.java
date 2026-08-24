package com.example.travel.domain.gathering.dto.response;

import com.example.travel.domain.gathering.enums.GatheringStatus;

public record CreateGatheringResponse(Long id, GatheringStatus status) {
}
