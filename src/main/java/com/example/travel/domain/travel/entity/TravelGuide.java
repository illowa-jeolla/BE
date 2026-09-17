package com.example.travel.domain.travel.entity;

import com.example.travel.domain.travel.enums.GuideStatus;
import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.UpdatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "travel_guides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelGuide extends UpdatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_request_id", nullable = false, unique = true)
    private Long sourceRequestId;

    @OneToOne(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY, optional = false)
    private TravelGuideCondition condition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "travel_tip", columnDefinition = "TEXT")
    private String travelTip;

    @Column(name = "generated_by_ai")
    private boolean generatedByAi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideStatus status = GuideStatus.DRAFT;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    private TravelGuide(TravelRecommendationContext request, User user, String title,
                        String summary, String travelTip, boolean generatedByAi,
                        OffsetDateTime generatedAt) {
        this.sourceRequestId = request.getId();
        this.user = user;
        this.title = title;
        this.summary = summary;
        this.travelTip = travelTip;
        this.generatedByAi = generatedByAi;
        this.status = GuideStatus.READY;
        this.generatedAt = generatedAt;
        this.condition = TravelGuideCondition.create(this, request);
    }

    public static TravelGuide ready(TravelRecommendationContext request, User user, String title,
                                    String summary, String travelTip, boolean generatedByAi,
                                    OffsetDateTime generatedAt) {
        return new TravelGuide(request, user, title, summary, travelTip,
                generatedByAi, generatedAt);
    }

}
