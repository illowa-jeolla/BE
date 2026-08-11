package com.example.travel.domain.ai;

import com.example.travel.domain.region.Region;
import com.example.travel.domain.user.User;
import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "ai_match_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMatchRequest extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "desired_lifestyle", length = 255)
    private String desiredLifestyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_region_id")
    private Region preferredRegion;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "job_interests", nullable = false, columnDefinition = "text[]")
    private String[] jobInterests = new String[0];

    @Column(name = "desired_salary")
    private Integer desiredSalary;

    @Column(name = "stay_period", length = 100)
    private String stayPeriod;

    @Column(name = "has_vehicle")
    private Boolean hasVehicle;

    @Column(name = "extra_conditions", columnDefinition = "TEXT")
    private String extraConditions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiRequestStatus status = AiRequestStatus.PENDING;
}
