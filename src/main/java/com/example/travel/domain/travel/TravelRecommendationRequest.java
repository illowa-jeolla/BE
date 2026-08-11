package com.example.travel.domain.travel;

import com.example.travel.domain.region.Region;
import com.example.travel.domain.user.User;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "travel_recommendation_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelRecommendationRequest extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "lodging_name", length = 150)
    private String lodgingName;

    @Column(name = "lodging_address", length = 255)
    private String lodgingAddress;

    @Column(name = "lodging_latitude", precision = 10, scale = 7)
    private BigDecimal lodgingLatitude;

    @Column(name = "lodging_longitude", precision = 10, scale = 7)
    private BigDecimal lodgingLongitude;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] themes = new String[0];

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", length = 20)
    private TransportType transportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "companion_type", length = 20)
    private CompanionType companionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status = RecommendationStatus.PENDING;
}
