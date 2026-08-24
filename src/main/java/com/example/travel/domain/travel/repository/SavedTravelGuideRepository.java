package com.example.travel.domain.travel.repository;

import com.example.travel.domain.travel.entity.SavedTravelGuide;
import com.example.travel.domain.travel.entity.id.SavedTravelGuideId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SavedTravelGuideRepository
        extends JpaRepository<SavedTravelGuide, SavedTravelGuideId> {

    @EntityGraph(attributePaths = {"guide", "guide.condition"})
    List<SavedTravelGuide> findByUserIdAndDeletedAtIsNullOrderBySavedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"guide", "guide.condition"})
    List<SavedTravelGuide> findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long userId);

    Optional<SavedTravelGuide> findByIdAndDeletedAtIsNull(SavedTravelGuideId id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SavedTravelGuide saved where saved.deletedAt is not null")
    int deleteAllSoftDeleted();
}
