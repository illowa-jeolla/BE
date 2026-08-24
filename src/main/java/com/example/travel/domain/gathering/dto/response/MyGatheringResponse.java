package com.example.travel.domain.gathering.dto.response;

import com.example.travel.domain.gathering.dto.item.MyGatheringItem;

import java.util.List;

public record MyGatheringResponse(
        String type,
        List<MyGatheringItem> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
