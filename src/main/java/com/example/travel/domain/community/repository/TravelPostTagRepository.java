package com.example.travel.domain.community.repository;

import com.example.travel.domain.community.entity.TravelPostTag;
import com.example.travel.domain.community.entity.id.TravelPostTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelPostTagRepository extends JpaRepository<TravelPostTag, TravelPostTagId> {
    @Modifying
    @Query("delete from TravelPostTag t where t.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
