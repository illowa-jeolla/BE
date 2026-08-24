package com.example.travel.domain.gathering.entity.id;

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
public class GatheringParticipantId implements Serializable {
    @Column(name = "gathering_id")
    private Long gatheringId;

    @Column(name = "user_id")
    private Long userId;
}
