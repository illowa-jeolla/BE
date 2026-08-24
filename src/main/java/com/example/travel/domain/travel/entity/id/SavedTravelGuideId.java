package com.example.travel.domain.travel.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SavedTravelGuideId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "guide_id")
    private Long guideId;
}
