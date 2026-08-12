package com.example.travel.domain.ai.entity.id;

import com.example.travel.domain.ai.enums.PriorityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AiMatchPriorityId implements Serializable {
    @Column(name = "request_id")
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_type", length = 30)
    private PriorityType priorityType;
}
