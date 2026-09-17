package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.dto.response.AiMatchResultResponse;
import com.example.travel.domain.ai.entity.AiMatchReport;
import com.example.travel.domain.ai.repository.AiMatchReportRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiMatchPersistenceService {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private final AiMatchReportRepository repository;
    private final UserRepository userRepository;

    public AiMatchPersistenceService(AiMatchReportRepository repository, UserRepository userRepository) {
        this.repository = repository; this.userRepository = userRepository;
    }

    @Transactional
    public void save(UUID requestId, Long userId, AiMatchResultResponse response) {
        if (repository.existsByRequestId(requestId)) return;
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).orElseThrow();
        try { repository.save(AiMatchReport.create(requestId, user, MAPPER.writeValueAsString(response))); }
        catch (Exception exception) { throw new IllegalStateException("AI match result serialization failed", exception); }
    }
}
