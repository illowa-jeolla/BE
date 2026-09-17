package com.example.travel.domain.community.repository;

import com.example.travel.domain.community.entity.TravelPostLike;
import com.example.travel.domain.community.entity.id.TravelPostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelPostLikeRepository extends JpaRepository<TravelPostLike, TravelPostLikeId> {
    long countByPostId(Long postId);

    @Modifying
    @Query("delete from TravelPostLike l where l.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
