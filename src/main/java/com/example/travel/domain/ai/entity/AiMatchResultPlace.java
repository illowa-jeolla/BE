package com.example.travel.domain.ai.entity;

import com.example.travel.domain.ai.entity.id.AiMatchResultPlaceId;
import com.example.travel.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_match_result_places", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_result_place_order", columnNames = {"result_id", "display_order"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMatchResultPlace {
    @EmbeddedId
    private AiMatchResultPlaceId id;

    @MapsId("resultId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private AiMatchResult result;

    @MapsId("placeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "display_order", nullable = false)
    private short displayOrder = 1;
}
