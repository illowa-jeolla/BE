package com.example.travel.domain.job.repository;

import com.example.travel.domain.job.entity.JobApplication;
import com.example.travel.domain.job.enums.JobFavoriteSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    Optional<JobApplication> findByUserIdAndSourceAndExternalId(
            Long userId, JobFavoriteSource source, String externalId);

    Page<JobApplication> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);
}
