package com.example.travel.domain.community;

import com.example.travel.domain.region.Region;
import com.example.travel.domain.user.User;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String concept;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelPostStatus status = TravelPostStatus.PUBLISHED;

    @Column(name = "view_count", nullable = false)
    private int viewCount;
}
