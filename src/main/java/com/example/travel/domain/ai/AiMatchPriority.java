package com.example.travel.domain.ai;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_match_priorities", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_priority_order", columnNames = {"request_id", "priority_order"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMatchPriority {
    @EmbeddedId
    private AiMatchPriorityId id;

    @MapsId("requestId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private AiMatchRequest request;

    @Column(name = "priority_order", nullable = false)
    private short priorityOrder;
}
