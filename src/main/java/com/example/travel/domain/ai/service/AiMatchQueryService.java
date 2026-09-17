package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.dto.response.AiMatchResultResponse;
import com.example.travel.domain.ai.entity.AiMatchReport;
import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.exception.AiMatchErrorCode;
import com.example.travel.domain.ai.exception.AiMatchException;
import com.example.travel.domain.ai.repository.AiMatchReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiMatchQueryService {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private final AiMatchReportRepository repository;
    private final AiMatchRequestCacheService cacheService;

    public AiMatchQueryService(AiMatchReportRepository repository, AiMatchRequestCacheService cacheService) {
        this.repository = repository; this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public AiMatchResultResponse find(Long userId, UUID requestId) {
        return repository.findByRequestIdAndUserId(requestId, userId).map(this::decode).orElseGet(() -> {
            var context = cacheService.find(requestId)
                    .filter(value -> value.userId().equals(userId))
                    .orElseThrow(() -> new AiMatchException(AiMatchErrorCode.REQUEST_NOT_FOUND));
            return AiMatchResultResponse.processing(requestId, context.status());
        });
    }

    @Transactional(readOnly = true)
    public Page<AiMatchResultResponse> findAll(Long userId, Pageable pageable) {
        return repository.findAllByUserIdOrderByIdDesc(userId, pageable).map(this::decode);
    }

    @Transactional
    public void delete(Long userId, UUID requestId) {
        AiMatchReport report = repository.findByRequestIdAndUserId(requestId, userId)
                .orElseThrow(() -> new AiMatchException(AiMatchErrorCode.RESULT_NOT_FOUND));
        repository.delete(report);
    }

    private AiMatchResultResponse decode(AiMatchReport report) {
        try { return MAPPER.readValue(report.getResultJson(), AiMatchResultResponse.class); }
        catch (Exception exception) { throw new AiMatchException(AiMatchErrorCode.INVALID_AI_RESPONSE, exception); }
    }
}
