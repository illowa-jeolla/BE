package com.example.travel.domain.ai.repository;

import com.example.travel.domain.ai.entity.AiMatchReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiMatchReportRepository extends JpaRepository<AiMatchReport, Long> {
    Optional<AiMatchReport> findByRequestIdAndUserId(UUID requestId, Long userId);
    Page<AiMatchReport> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    boolean existsByRequestId(UUID requestId);
}
