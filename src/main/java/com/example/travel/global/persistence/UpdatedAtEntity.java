package com.example.travel.global.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
public abstract class UpdatedAtEntity extends CreatedAtEntity {
    @Column(name = "updated_at", nullable = false, insertable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;

    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = OffsetDateTime.now();
    }

    protected void touchUpdatedAt() {
        updatedAt = OffsetDateTime.now();
    }
}
