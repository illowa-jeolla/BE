package com.example.travel.domain.travel.entity;

import com.example.travel.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "travel_guide_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_guide_day_order", columnNames = {"guide_id", "day_number", "item_order"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelGuideItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private TravelGuide guide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "tour_content_id", length = 30)
    private String tourContentId;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "day_number", nullable = false)
    private short dayNumber;

    @Column(name = "item_order", nullable = false)
    private short itemOrder;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at")
    private LocalTime startsAt;

    @Column(name = "ends_at")
    private LocalTime endsAt;

    @Column(name = "travel_minutes")
    private Integer travelMinutes;

    @Column(name = "stay_minutes")
    private Integer stayMinutes;

    private TravelGuideItem(TravelGuide guide, String tourContentId, short dayNumber,
                            short itemOrder, String title, String description,
                            LocalTime startsAt, Integer stayMinutes, BigDecimal latitude,
                            BigDecimal longitude, String thumbnailUrl) {
        this.guide = guide;
        this.tourContentId = tourContentId;
        this.dayNumber = dayNumber;
        this.itemOrder = itemOrder;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = startsAt == null || stayMinutes == null
                ? null : startsAt.plusMinutes(stayMinutes);
        this.stayMinutes = stayMinutes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static TravelGuideItem create(TravelGuide guide, String tourContentId,
                                         short dayNumber, short itemOrder, String title,
                                         String description, LocalTime startsAt,
                                         Integer stayMinutes, BigDecimal latitude,
                                         BigDecimal longitude, String thumbnailUrl) {
        return new TravelGuideItem(guide, tourContentId, dayNumber, itemOrder, title,
                description, startsAt, stayMinutes, latitude, longitude, thumbnailUrl);
    }

    public void setTravelMinutes(Integer travelMinutes) {
        this.travelMinutes = travelMinutes;
    }
}
