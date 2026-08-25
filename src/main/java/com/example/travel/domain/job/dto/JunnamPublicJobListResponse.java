package com.example.travel.domain.job.dto;

import java.util.List;

public record JunnamPublicJobListResponse(
        int startPage,
        int pageSize,
        int numOfRows,
        int totalCount,
        List<JunnamPublicJobItem> items
) {
}
