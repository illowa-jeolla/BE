package com.example.travel.domain.community.repository;

import com.example.travel.domain.community.entity.TravelPostComment;
import com.example.travel.domain.community.enums.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TravelPostCommentRepository extends JpaRepository<TravelPostComment, Long> {
    long countByPostIdAndStatusIn(Long postId, Collection<CommentStatus> statuses);
    List<TravelPostComment> findAllByPostIdAndStatusInOrderByCreatedAtAscIdAsc(
            Long postId, Collection<CommentStatus> statuses);
    Optional<TravelPostComment> findByIdAndPostId(Long commentId, Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from TravelPostComment c
            join fetch c.author
            join fetch c.post p
            join fetch p.author
            where c.id = :commentId and p.id = :postId and c.status <> :deleted
            """)
    Optional<TravelPostComment> findActiveForUpdate(@Param("commentId") Long commentId,
                                                    @Param("postId") Long postId,
                                                    @Param("deleted") CommentStatus deleted);
}
