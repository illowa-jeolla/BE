package com.example.travel.domain.ai;

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
public class AiMatchResultPlaceId implements Serializable {
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "place_id")
    private Long placeId;
}
