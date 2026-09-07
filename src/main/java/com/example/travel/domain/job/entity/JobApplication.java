package com.example.travel.domain.job.entity;

import com.example.travel.domain.job.enums.JobApplicationStatus;
import com.example.travel.domain.job.enums.JobFavoriteSource;
import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.UpdatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "job_applications", uniqueConstraints =
        @UniqueConstraint(name = "uk_job_application_user_source_external",
                columnNames = {"user_id", "source", "external_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobApplication extends UpdatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobFavoriteSource source;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String deadline;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobApplicationStatus status;

    public static JobApplication create(User user, JobFavoriteSource source, String externalId,
                                        String title, String companyName, String address,
                                        String deadline, String sourceUrl) {
        JobApplication application = new JobApplication();
        application.user = user;
        application.source = source;
        application.externalId = externalId;
        application.title = title;
        application.companyName = companyName;
        application.address = address;
        application.deadline = deadline;
        application.sourceUrl = sourceUrl;
        application.status = JobApplicationStatus.APPLIED;
        return application;
    }

    public void updateStatus(JobApplicationStatus status) {
        this.status = status;
        touchUpdatedAt();
    }
}
