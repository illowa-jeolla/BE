package com.example.travel.domain.ai.dto.request;

import com.example.travel.domain.ai.enums.PriorityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAiMatchRequest(
        @NotNull Long preferredRegionId,
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 100) String> desiredJobs,
        @NotEmpty @Size(min = 4, max = 4) List<@NotNull PriorityType> priorities,
        @NotBlank @Size(max = 100) String thought
) {
    public CreateAiMatchRequest {
        desiredJobs = desiredJobs == null ? null : desiredJobs.stream().map(String::trim).toList();
        thought = thought == null ? null : thought.trim();
    }
}
