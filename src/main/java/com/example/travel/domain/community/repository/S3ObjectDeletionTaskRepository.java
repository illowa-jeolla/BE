package com.example.travel.domain.community.repository;

import com.example.travel.domain.community.entity.S3ObjectDeletionTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface S3ObjectDeletionTaskRepository extends JpaRepository<S3ObjectDeletionTask, Long> {
    Optional<S3ObjectDeletionTask> findByObjectKey(String objectKey);

    List<S3ObjectDeletionTask> findByNextAttemptAtLessThanEqualOrderByIdAsc(
            OffsetDateTime now, Pageable pageable);
}
