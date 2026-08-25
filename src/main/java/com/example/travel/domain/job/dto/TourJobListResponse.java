package com.example.travel.domain.job.dto;

import java.util.List;

public record TourJobListResponse(
        int pageNo,
        int numOfRows,
        int totalCount,
        List<TourJobItem> items
) {
}
