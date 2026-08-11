package com.example.travel.domain.travel;

import com.example.travel.domain.place.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

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
}
