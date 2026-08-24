package com.example.travel.domain.travel.dto.response;

import java.util.List;

public record ManualTravelPlacePageResponse(
        int pageNo,
        int pageSize,
        int totalCount,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext,
        List<ManualTravelPlaceItem> items
) {}
