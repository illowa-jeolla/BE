package com.example.travel.domain.travel;

import com.example.travel.domain.place.Place;
import com.example.travel.domain.user.User;
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
