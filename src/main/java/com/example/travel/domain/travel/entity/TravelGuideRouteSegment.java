package com.example.travel.domain.travel.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "travel_guide_route_segments", uniqueConstraints =
        @UniqueConstraint(name = "uk_guide_day_segment_order",
                columnNames = {"guide_id", "day_number", "segment_order"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelGuideRouteSegment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private TravelGuide guide;
    @Column(name = "day_number", nullable = false) private short dayNumber;
    @Column(name = "segment_order", nullable = false) private short segmentOrder;
    @Column(name = "from_name", nullable = false, length = 200) private String fromName;
    @Column(name = "to_name", nullable = false, length = 200) private String toName;
    @Column(name = "from_latitude", precision = 10, scale = 7) private BigDecimal fromLatitude;
    @Column(name = "from_longitude", precision = 10, scale = 7) private BigDecimal fromLongitude;
    @Column(name = "to_latitude", precision = 10, scale = 7) private BigDecimal toLatitude;
    @Column(name = "to_longitude", precision = 10, scale = 7) private BigDecimal toLongitude;
    @Column(name = "distance_meters", nullable = false) private int distanceMeters;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(name = "estimated", nullable = false) private boolean estimated;
    @Column(name = "path_json", columnDefinition = "TEXT") private String pathJson;

    public static TravelGuideRouteSegment create(TravelGuide guide, short dayNumber,
            short segmentOrder, String fromName, String toName,
            BigDecimal fromLatitude, BigDecimal fromLongitude,
            BigDecimal toLatitude, BigDecimal toLongitude,
            int distanceMeters, int durationMinutes, boolean estimated, String pathJson) {
        TravelGuideRouteSegment value = new TravelGuideRouteSegment();
        value.guide = guide; value.dayNumber = dayNumber; value.segmentOrder = segmentOrder;
        value.fromName = fromName; value.toName = toName;
        value.fromLatitude = fromLatitude; value.fromLongitude = fromLongitude;
        value.toLatitude = toLatitude; value.toLongitude = toLongitude;
        value.distanceMeters = distanceMeters; value.durationMinutes = durationMinutes;
        value.estimated = estimated; value.pathJson = pathJson;
        return value;
    }
}
