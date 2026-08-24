package com.example.travel.domain.travel.repository;

import com.example.travel.domain.travel.entity.TravelGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TravelGuideRepository extends JpaRepository<TravelGuide, Long> {
    Optional<TravelGuide> findBySourceRequestId(Long requestId);
    Optional<TravelGuide> findBySourceRequestIdAndUserId(Long requestId, Long userId);
    Optional<TravelGuide> findByIdAndUserId(Long id, Long userId);

}
