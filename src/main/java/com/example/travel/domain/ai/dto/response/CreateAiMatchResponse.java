package com.example.travel.domain.ai.dto.response;

import com.example.travel.domain.ai.enums.AiRequestStatus;

import java.util.UUID;

public record CreateAiMatchResponse(UUID requestId, AiRequestStatus status) {}
