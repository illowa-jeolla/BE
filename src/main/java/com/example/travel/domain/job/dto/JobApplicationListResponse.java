package com.example.travel.domain.job.dto;

import java.util.List;

public record JobApplicationListResponse(
        List<JobApplicationItem> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {}
