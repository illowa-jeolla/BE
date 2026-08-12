package com.example.travel.domain.travel.entity;

import com.example.travel.domain.place.entity.Place;
import com.example.travel.domain.travel.entity.id.VisitedPlaceId;
import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "visited_places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitedPlace extends CreatedAtEntity {
    @EmbeddedId
    private VisitedPlaceId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("placeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(columnDefinition = "TEXT")
    private String memo;
}
