package com.example.travel.domain.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "s3_object_deletion_tasks", uniqueConstraints =
        @UniqueConstraint(name = "uk_s3_object_deletion_task_key", columnNames = "object_key"))
public class S3ObjectDeletionTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected S3ObjectDeletionTask() {
    }

    private S3ObjectDeletionTask(String objectKey) {
        this.objectKey = objectKey;
        this.nextAttemptAt = OffsetDateTime.now();
        this.createdAt = OffsetDateTime.now();
    }

    public static S3ObjectDeletionTask create(String objectKey) {
        return new S3ObjectDeletionTask(objectKey);
    }

    public Long getId() { return id; }
    public String getObjectKey() { return objectKey; }
    public int getAttemptCount() { return attemptCount; }

    public void recordFailure(String error, OffsetDateTime nextAttemptAt) {
        this.attemptCount++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        this.nextAttemptAt = nextAttemptAt;
    }
}
