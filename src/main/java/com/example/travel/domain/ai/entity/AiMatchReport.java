package com.example.travel.domain.ai.entity;

import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "ai_match_reports", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_match_report_request", columnNames = "request_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMatchReport extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;

    public static AiMatchReport create(UUID requestId, User user, String resultJson) {
        AiMatchReport report = new AiMatchReport();
        report.requestId = requestId;
        report.user = user; report.resultJson = resultJson;
        return report;
    }
}
