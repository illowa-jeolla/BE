package com.example.travel.domain.job.entity;

import com.example.travel.domain.job.enums.JobFavoriteSource;
import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "job_favorites", uniqueConstraints =
        @UniqueConstraint(name = "uk_job_favorite_user_source_external",
                columnNames = {"user_id", "source", "external_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobFavorite extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public static JobFavorite create(User user, JobFavoriteSource source, String externalId,
                                     String title, String companyName, String address,
                                     String deadline, String sourceUrl) {
        JobFavorite favorite = new JobFavorite();
        favorite.user = user; favorite.source = source; favorite.externalId = externalId;
        favorite.title = title; favorite.companyName = companyName; favorite.address = address;
        favorite.deadline = deadline; favorite.sourceUrl = sourceUrl;
        return favorite;
    }
}
