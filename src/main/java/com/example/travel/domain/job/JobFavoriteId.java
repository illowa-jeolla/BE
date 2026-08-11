package com.example.travel.domain.job;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class JobFavoriteId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "job_posting_id")
    private Long jobPostingId;
}
