package com.example.travel.domain.ai.controller;

import com.example.travel.domain.ai.dto.request.CreateAiMatchRequest;
import com.example.travel.domain.ai.dto.response.AiMatchResultResponse;
import com.example.travel.domain.ai.dto.response.CreateAiMatchResponse;
import com.example.travel.domain.ai.service.AiMatchQueryService;
import com.example.travel.domain.ai.service.AiMatchService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-matches")
public class AiMatchController {
    private final AiMatchService service;
    private final AiMatchQueryService queryService;
    public AiMatchController(AiMatchService service, AiMatchQueryService queryService) {
        this.service = service; this.queryService = queryService;
    }
    @PostMapping
    public ResponseEntity<ApiResponse<CreateAiMatchResponse>> create(Authentication authentication,
            @Valid @RequestBody CreateAiMatchRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(service.create((Long) authentication.getPrincipal(), request)));
    }
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<AiMatchResultResponse>> find(Authentication authentication,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(ApiResponse.success(queryService.find((Long) authentication.getPrincipal(), requestId)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AiMatchResultResponse>>> findAll(Authentication authentication,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(queryService.findAll((Long) authentication.getPrincipal(), pageable)));
    }
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID requestId) {
        queryService.delete((Long) authentication.getPrincipal(), requestId);
        return ResponseEntity.noContent().build();
    }
}
