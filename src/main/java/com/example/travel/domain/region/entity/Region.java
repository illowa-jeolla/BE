package com.example.travel.domain.region.entity;

import com.example.travel.domain.region.enums.RegionType;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "regions", uniqueConstraints =
        @UniqueConstraint(name = "uk_region_parent_name", columnNames = {"parent_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Region parent;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", nullable = false, length = 20)
    private RegionType regionType = RegionType.CITY;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    private Region(String name, RegionType regionType, BigDecimal latitude,
                   BigDecimal longitude) {
        this.name = name;
        this.regionType = regionType;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Region createSupportedCity(String name, BigDecimal latitude,
                                             BigDecimal longitude) {
        return new Region(name, RegionType.CITY, latitude, longitude);
    }
}
