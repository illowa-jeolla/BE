package com.example.travel.domain.ai.entity;

import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import com.example.travel.domain.region.entity.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "ai_job_candidates", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_job_source_external", columnNames = {"source", "external_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJobCandidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ExternalCandidateSource source;
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "region_id")
    private Region region;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(name = "company_name", length = 255)
    private String companyName;
    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;
    @Column(name = "employment_type", length = 100)
    private String employmentType;
    @Column(name = "salary_text", length = 255)
    private String salaryText;
    @Column(name = "posted_at")
    private LocalDate postedAt;
    @Column(name = "deadline")
    private LocalDate deadline;
    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;
    @JdbcTypeCode(SqlTypes.VECTOR) @Column(columnDefinition = "vector(1536)")
    private float[] embedding;
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;
    @Column(name = "embedding_source_hash", length = 64)
    private String embeddingSourceHash;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;
    @Column(name = "inactive_at")
    private OffsetDateTime inactiveAt;
    @Column(name = "embedded_at")
    private OffsetDateTime embeddedAt;

    public static AiJobCandidate create(ExternalCandidateSource source, String externalId, Region region,
                                        String title, String companyName, String address, String description,
                                        String employmentType, String salaryText, LocalDate postedAt,
                                        LocalDate deadline, String sourceUrl, OffsetDateTime now) {
        AiJobCandidate value = new AiJobCandidate(); value.source = source; value.externalId = externalId;
        value.refresh(region, title, companyName, address, description, employmentType, salaryText,
                postedAt, deadline, sourceUrl, now); return value;
    }

    public void refresh(Region region, String title, String companyName, String address, String description,
                        String employmentType, String salaryText, LocalDate postedAt, LocalDate deadline,
                        String sourceUrl, OffsetDateTime now) {
        this.region = region; this.title = title; this.companyName = companyName; this.address = address;
        this.jobDescription = description; this.employmentType = employmentType; this.salaryText = salaryText;
        this.deadline = deadline; this.postedAt = postedAt; this.sourceUrl = sourceUrl;
        this.active = true; this.lastSeenAt = now;
        this.inactiveAt = null;
    }

    public boolean requiresEmbedding(String hash, String model) {
        return embedding == null || !hash.equals(embeddingSourceHash) || !model.equals(embeddingModel);
    }

    public void updateEmbedding(float[] embedding, String model, String hash, OffsetDateTime now) {
        this.embedding = embedding; this.embeddingModel = model; this.embeddingSourceHash = hash; this.embeddedAt = now;
    }

    public void invalidateEmbedding() {
        this.embedding = null; this.embeddingModel = null;
        this.embeddingSourceHash = null; this.embeddedAt = null;
    }

    public void deactivate(OffsetDateTime now) { this.active = false; this.inactiveAt = now; }
}
