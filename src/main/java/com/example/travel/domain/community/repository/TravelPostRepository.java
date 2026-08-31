package com.example.travel.domain.community.repository;

import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.enums.TravelPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.List;

public interface TravelPostRepository extends JpaRepository<TravelPost, Long> {
    Optional<TravelPost> findFirstByAuthorIdAndStatusOrderByIdDesc(
            Long authorId, TravelPostStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from TravelPost p
            left join fetch p.region
            where p.id = :draftId
              and p.author.id = :authorId
              and p.status = :status
            """)
    Optional<TravelPost> findOwnedByIdAndStatusForUpdate(
            @Param("draftId") Long draftId,
            @Param("authorId") Long authorId,
            @Param("status") TravelPostStatus status);

    @Query("""
            select p from TravelPost p
            join fetch p.author
            join fetch p.region
            where p.status = :status
              and (:regionId is null or p.region.id = :regionId)
            """)
    Page<TravelPost> findPublished(@Param("status") TravelPostStatus status,
                                   @Param("regionId") Long regionId,
                                   Pageable pageable);

    @Query("""
            select p from TravelPost p
            join fetch p.author
            join fetch p.region
            where p.id = :postId and p.status = :status
            """)
    Optional<TravelPost> findDetail(@Param("postId") Long postId,
                                    @Param("status") TravelPostStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TravelPost p where p.id = :postId and p.status = :status")
    Optional<TravelPost> findByIdAndStatusForUpdate(@Param("postId") Long postId,
                                                    @Param("status") TravelPostStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TravelPost p set p.viewCount = p.viewCount + 1 "
            + "where p.id = :postId and p.status = :status")
    int incrementViewCount(@Param("postId") Long postId,
                           @Param("status") TravelPostStatus status);

    List<TravelPost> findAllByStatusAndUpdatedAtBefore(TravelPostStatus status,
                                                       OffsetDateTime cutoff);
}
