package com.example.travel.domain.job.dto;

import com.example.travel.domain.job.enums.JobFavoriteSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateJobApplicationRequest(
        @NotNull JobFavoriteSource source,
        @NotBlank @Size(max = 100) String externalId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String companyName,
        String address,
        @Size(max = 50) String deadline,
        String sourceUrl
) {}
