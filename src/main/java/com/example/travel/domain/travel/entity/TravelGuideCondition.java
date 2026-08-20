package com.example.travel.domain.travel.entity;

import com.example.travel.domain.travel.enums.CompanionType;
import com.example.travel.domain.travel.enums.TransportType;
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
@Table(name = "travel_guide_conditions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelGuideCondition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false, unique = true) private TravelGuide guide;
    @Column(name = "region_id") private Long regionId;
    @Column(name = "region_name", nullable = false, length = 80) private String regionName;
    @Column(name = "lodging_kakao_place_id", length = 100) private String lodgingKakaoPlaceId;
    @Column(name = "lodging_name", length = 150) private String lodgingName;
    @Column(name = "lodging_address") private String lodgingAddress;
    @Column(name = "lodging_latitude", precision = 10, scale = 7) private BigDecimal lodgingLatitude;
    @Column(name = "lodging_longitude", precision = 10, scale = 7) private BigDecimal lodgingLongitude;
    @Column(name = "start_place_id", length = 100) private String startPlaceId;
    @Column(name = "start_place_name", length = 150) private String startPlaceName;
    @Column(name = "start_place_address") private String startPlaceAddress;
    @Column(name = "start_latitude", precision = 10, scale = 7) private BigDecimal startLatitude;
    @Column(name = "start_longitude", precision = 10, scale = 7) private BigDecimal startLongitude;
    @Column(name = "end_place_id", length = 100) private String endPlaceId;
    @Column(name = "end_place_name", length = 150) private String endPlaceName;
    @Column(name = "end_place_address") private String endPlaceAddress;
    @Column(name = "end_latitude", precision = 10, scale = 7) private BigDecimal endLatitude;
    @Column(name = "end_longitude", precision = 10, scale = 7) private BigDecimal endLongitude;
    @Column(name = "starts_on", nullable = false) private LocalDate startsOn;
    @Column(name = "ends_on", nullable = false) private LocalDate endsOn;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(columnDefinition = "text[]") private String[] themes;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "daily_place_counts", columnDefinition = "integer[]")
    private Integer[] dailyPlaceCounts;
    @Enumerated(EnumType.STRING) @Column(name = "transport_type", length = 20) private TransportType transportType;
    @Enumerated(EnumType.STRING) @Column(name = "companion_type", length = 20) private CompanionType companionType;

    static TravelGuideCondition create(TravelGuide guide, TravelRecommendationRequest request) {
        TravelGuideCondition value = new TravelGuideCondition();
        value.guide = guide; value.regionId = request.getRegionId(); value.regionName = request.getRegionName();
        value.lodgingKakaoPlaceId = request.getLodgingKakaoPlaceId(); value.lodgingName = request.getLodgingName();
        value.lodgingAddress = request.getLodgingAddress(); value.lodgingLatitude = request.getLodgingLatitude();
        value.lodgingLongitude = request.getLodgingLongitude(); value.startPlaceId = request.getStartPlaceId();
        value.startPlaceName = request.getStartPlaceName(); value.startPlaceAddress = request.getStartPlaceAddress();
        value.startLatitude = request.getStartLatitude(); value.startLongitude = request.getStartLongitude();
        value.endPlaceId = request.getEndPlaceId(); value.endPlaceName = request.getEndPlaceName();
        value.endPlaceAddress = request.getEndPlaceAddress(); value.endLatitude = request.getEndLatitude();
        value.endLongitude = request.getEndLongitude(); value.startsOn = request.getStartsOn();
        value.endsOn = request.getEndsOn(); value.themes = request.getThemes();
        value.dailyPlaceCounts = request.getDailyPlaceCounts(); value.transportType = request.getTransportType();
        value.companionType = request.getCompanionType(); return value;
    }

}
