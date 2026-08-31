package com.example.travel.domain.community.repository;

import com.example.travel.domain.community.entity.TravelPostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelPostImageRepository extends JpaRepository<TravelPostImage, Long> {
    long countByPostId(Long postId);

    List<TravelPostImage> findAllByPostIdOrderByDisplayOrderAsc(Long postId);

    Optional<TravelPostImage> findByIdAndPostId(Long imageId, Long postId);

    void deleteAllByPostId(Long postId);
}
