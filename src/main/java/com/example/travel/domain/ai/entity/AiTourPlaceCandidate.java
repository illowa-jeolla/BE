package com.example.travel.domain.ai.entity;

import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import com.example.travel.domain.region.entity.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "ai_tour_place_candidates", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_tour_place_source_external", columnNames = {"source", "external_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTourPlaceCandidate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ExternalCandidateSource source;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "region_id")
    private Region region;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

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

    public static AiTourPlaceCandidate create(String externalId, Region region, String name,
                                               String category, String address, String description,
                                               String imageUrl, BigDecimal latitude, BigDecimal longitude,
                                               OffsetDateTime now) {
        AiTourPlaceCandidate value = new AiTourPlaceCandidate();
        value.source = ExternalCandidateSource.TOUR_INFO;
        value.externalId = externalId;
        value.refresh(region, name, category, address, description, imageUrl, latitude, longitude, now);
        return value;
    }

    public void refresh(Region region, String name, String category, String address, String description,
                        String imageUrl, BigDecimal latitude, BigDecimal longitude, OffsetDateTime now) {
        this.region = region;
        this.name = name;
        this.category = category;
        this.address = address;
        this.description = description;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
        this.lastSeenAt = now;
        this.inactiveAt = null;
    }

    public boolean requiresEmbedding(String hash, String model) {
        return embedding == null || !hash.equals(embeddingSourceHash) || !model.equals(embeddingModel);
    }

    public void updateEmbedding(float[] embedding, String model, String hash, OffsetDateTime now) {
        this.embedding = embedding;
        this.embeddingModel = model;
        this.embeddingSourceHash = hash;
        this.embeddedAt = now;
    }

    public void deactivate(OffsetDateTime now) {
        this.active = false;
        this.inactiveAt = now;
    }
}
