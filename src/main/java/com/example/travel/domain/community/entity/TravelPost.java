package com.example.travel.domain.community.entity;

import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.user.entity.User;
import com.example.travel.global.persistence.UpdatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "travel_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPost extends UpdatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(length = 200)
    private String title;

    @Column(length = 100)
    private String concept;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelPostStatus status = TravelPostStatus.PUBLISHED;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    private TravelPost(User author) {
        this.author = author;
        this.status = TravelPostStatus.DRAFT;
    }

    public static TravelPost createDraft(User author) {
        return new TravelPost(author);
    }

    public void updateDraft(Region region, String title, String concept, String content) {
        if (status != TravelPostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be updated as drafts.");
        }
        this.region = region;
        this.title = title;
        this.concept = concept;
        this.content = content;
        touchUpdatedAt();
    }

    public void touchDraft() {
        if (status != TravelPostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be touched as drafts.");
        }
        touchUpdatedAt();
    }

    public void publish() {
        if (status != TravelPostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be published.");
        }
        status = TravelPostStatus.PUBLISHED;
        touchUpdatedAt();
    }

    public void updatePublished(Region region, String title, String concept, String content) {
        if (status != TravelPostStatus.PUBLISHED) {
            throw new IllegalStateException("Only published posts can be updated.");
        }
        this.region = region;
        this.title = title;
        this.concept = concept;
        this.content = content;
        touchUpdatedAt();
    }

    public void touchPublished() {
        if (status != TravelPostStatus.PUBLISHED) {
            throw new IllegalStateException("Only published posts can be touched.");
        }
        touchUpdatedAt();
    }

    public void softDelete() {
        if (status == TravelPostStatus.DELETED) {
            return;
        }
        status = TravelPostStatus.DELETED;
        touchUpdatedAt();
    }
}
