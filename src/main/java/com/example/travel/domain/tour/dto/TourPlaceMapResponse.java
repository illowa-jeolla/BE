package com.example.travel.domain.tour.dto;

import java.util.List;

public record TourPlaceMapResponse(
        int pageNo,
        int numOfRows,
        int totalCount,
        List<TourPlaceItem> items
) {}
