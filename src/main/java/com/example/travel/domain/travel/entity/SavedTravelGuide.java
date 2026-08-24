package com.example.travel.domain.travel.entity;

import com.example.travel.domain.travel.entity.id.SavedTravelGuideId;
import com.example.travel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "saved_travel_guides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedTravelGuide {
    @EmbeddedId
    private SavedTravelGuideId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("guideId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private TravelGuide guide;

    @Column(name = "saved_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private OffsetDateTime savedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    private SavedTravelGuide(User user, TravelGuide guide) {
        this.id = new SavedTravelGuideId(user.getId(), guide.getId());
        this.user = user;
        this.guide = guide;
    }

    public static SavedTravelGuide create(User user, TravelGuide guide) {
        return new SavedTravelGuide(user, guide);
    }

    public void restore() {
        this.deletedAt = null;
    }

    public void delete(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
