package com.example.travel.domain.job.dto;

import com.example.travel.domain.job.entity.JobFavorite;
import com.example.travel.domain.job.enums.JobFavoriteSource;
import java.time.OffsetDateTime;

public record JobFavoriteItem(Long favoriteId, JobFavoriteSource source, String externalId,
                              String title, String companyName, String address, String deadline,
                              String sourceUrl, OffsetDateTime favoritedAt) {
    public static JobFavoriteItem from(JobFavorite value) {
        return new JobFavoriteItem(value.getId(), value.getSource(), value.getExternalId(), value.getTitle(),
                value.getCompanyName(), value.getAddress(), value.getDeadline(), value.getSourceUrl(),
                value.getCreatedAt());
    }
}
