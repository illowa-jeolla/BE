package com.example.travel.domain.job.dto;

import java.util.Map;

public record JunnamPublicJobDetailResponse(
        String jobKey,
        String title,
        String writer,
        String categoryName,
        String insertedAt,
        String readCount,
        String content,
        String address,
        String tel,
        String homepageUrl,
        Map<String, String> rawFields
) {
}
