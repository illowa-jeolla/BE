package com.example.travel.domain.job;

import com.example.travel.domain.region.Region;
import com.example.travel.global.persistence.UpdatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "job_postings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosting extends UpdatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", length = 30)
    private WorkType workType;

    @Column(name = "preferred_time", length = 30)
    private String preferredTime;

    @Column(name = "work_schedule", length = 100)
    private String workSchedule;

    @Column(name = "work_hours", length = 100)
    private String workHours;

    @Column(name = "employment_period", length = 100)
    private String employmentPeriod;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Column(length = 255)
    private String address;

    @Column(name = "meeting_point", length = 150)
    private String meetingPoint;

    @Column(name = "parking_info", length = 150)
    private String parkingInfo;

    @Column(name = "transit_info", length = 150)
    private String transitInfo;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", length = 20)
    private SalaryType salaryType;

    @Column(name = "salary_amount", precision = 12, scale = 2)
    private BigDecimal salaryAmount;

    @Column(name = "salary_text", length = 100)
    private String salaryText;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private JobSourceType sourceType = JobSourceType.INTERNAL;

    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status = JobPostingStatus.DRAFT;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
}
