package com.example.travel.domain.job.dto;

import java.util.Map;

public record JunnamPublicJobItem(
        String companyName,
        String title,
        String address,
        String tel,
        String homepageUrl,
        Map<String, String> rawFields
) {
}
