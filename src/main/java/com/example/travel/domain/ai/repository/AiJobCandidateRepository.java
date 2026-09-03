package com.example.travel.domain.ai.repository;

import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AiJobCandidateRepository extends JpaRepository<AiJobCandidate, Long> {
    Optional<AiJobCandidate> findBySourceAndExternalId(ExternalCandidateSource source, String externalId);
    List<AiJobCandidate> findAllBySourceAndActiveTrueAndLastSeenAtBefore(
            ExternalCandidateSource source, OffsetDateTime startedAt);
    long deleteByActiveFalseAndInactiveAtBefore(OffsetDateTime cutoff);
}
