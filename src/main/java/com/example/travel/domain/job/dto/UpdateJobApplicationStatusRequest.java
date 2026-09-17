package com.example.travel.domain.job.dto;

import com.example.travel.domain.job.enums.JobApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateJobApplicationStatusRequest(@NotNull JobApplicationStatus status) {}
