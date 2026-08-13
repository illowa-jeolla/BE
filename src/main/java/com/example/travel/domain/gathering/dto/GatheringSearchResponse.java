package com.example.travel.domain.gathering.dto;

import java.util.List;

public record GatheringSearchResponse(
        List<GatheringSearchItem> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
