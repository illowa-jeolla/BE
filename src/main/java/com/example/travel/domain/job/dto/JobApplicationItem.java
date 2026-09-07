package com.example.travel.domain.job.dto;

import com.example.travel.domain.job.entity.JobApplication;
import com.example.travel.domain.job.enums.JobApplicationStatus;
import com.example.travel.domain.job.enums.JobFavoriteSource;

import java.time.OffsetDateTime;

public record JobApplicationItem(
        Long applicationId,
        JobFavoriteSource source,
        String externalId,
        String title,
        String companyName,
        String address,
        String deadline,
        String sourceUrl,
        JobApplicationStatus status,
        OffsetDateTime appliedAt,
        OffsetDateTime updatedAt
) {
    public static JobApplicationItem from(JobApplication value) {
        return new JobApplicationItem(value.getId(), value.getSource(), value.getExternalId(),
                value.getTitle(), value.getCompanyName(), value.getAddress(), value.getDeadline(),
                value.getSourceUrl(), value.getStatus(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
