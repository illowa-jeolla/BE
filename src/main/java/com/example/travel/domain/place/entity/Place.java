package com.example.travel.domain.place.entity;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.global.persistence.UpdatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends UpdatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;

    @Column(name = "average_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
