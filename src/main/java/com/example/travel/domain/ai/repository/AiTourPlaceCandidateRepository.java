package com.example.travel.domain.ai.repository;

import com.example.travel.domain.ai.entity.AiTourPlaceCandidate;
import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AiTourPlaceCandidateRepository extends JpaRepository<AiTourPlaceCandidate, Long> {
    @EntityGraph(attributePaths = "region")
    Optional<AiTourPlaceCandidate> findBySourceAndExternalId(ExternalCandidateSource source, String externalId);
    List<AiTourPlaceCandidate> findAllBySourceAndActiveTrueAndLastSeenAtBefore(
            ExternalCandidateSource source, OffsetDateTime startedAt);
    long deleteByActiveFalseAndInactiveAtBefore(OffsetDateTime cutoff);
}
