package com.example.travel.domain.job.repository;

import com.example.travel.domain.job.entity.JobFavorite;
import com.example.travel.domain.job.enums.JobFavoriteSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JobFavoriteRepository extends JpaRepository<JobFavorite, Long> {
    Optional<JobFavorite> findByUserIdAndSourceAndExternalId(Long userId, JobFavoriteSource source, String externalId);
    Page<JobFavorite> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    Optional<JobFavorite> findByIdAndUserId(Long id, Long userId);
}
