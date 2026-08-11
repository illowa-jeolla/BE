package com.example.travel.domain.ai;

import com.example.travel.domain.job.JobPosting;
import com.example.travel.domain.region.Region;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_match_results", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_result_rank", columnNames = {"request_id", "rank"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMatchResult extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private AiMatchRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;

    @Column(name = "overall_score", nullable = false)
    private short overallScore;

    @Column(name = "region_score")
    private Short regionScore;

    @Column(name = "job_score")
    private Short jobScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private short rank;
}
