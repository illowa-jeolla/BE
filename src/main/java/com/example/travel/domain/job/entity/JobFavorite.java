package com.example.travel.domain.job.entity;

import com.example.travel.domain.job.entity.id.JobFavoriteId;
import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "job_favorites")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobFavorite extends CreatedAtEntity {
    @EmbeddedId
    private JobFavoriteId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("jobPostingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;
}
