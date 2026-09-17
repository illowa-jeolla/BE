package com.example.travel.domain.ai.model;

import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.enums.PriorityType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiMatchRequestContext(UUID requestId, Long userId, Long preferredRegionId,
                                    List<String> desiredJobs, List<PriorityType> priorities,
                                    String thought, AiRequestStatus status,
                                    OffsetDateTime createdAt, String errorCode) {
    public AiMatchRequestContext withStatus(AiRequestStatus status, String errorCode) {
        return new AiMatchRequestContext(requestId, userId, preferredRegionId, desiredJobs,
                priorities, thought, status, createdAt, errorCode);
    }
}
