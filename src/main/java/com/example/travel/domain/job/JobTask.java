package com.example.travel.domain.job;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "job_tasks", uniqueConstraints =
        @UniqueConstraint(name = "uk_job_task_order", columnNames = {"job_posting_id", "display_order"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private short displayOrder = 1;
}
