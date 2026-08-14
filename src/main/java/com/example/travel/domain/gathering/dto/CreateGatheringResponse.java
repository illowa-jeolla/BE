package com.example.travel.domain.gathering.dto;

import com.example.travel.domain.gathering.enums.GatheringStatus;

public record CreateGatheringResponse(Long id, GatheringStatus status) {
}
