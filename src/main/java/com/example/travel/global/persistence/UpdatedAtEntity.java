package com.example.travel.global.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
public abstract class UpdatedAtEntity extends CreatedAtEntity {
    @Column(name = "updated_at", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void initializeTimestamp() {
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = OffsetDateTime.now();
    }

    protected void touchUpdatedAt() {
        updatedAt = OffsetDateTime.now();
    }
}
