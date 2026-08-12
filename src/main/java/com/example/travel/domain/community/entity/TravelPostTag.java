package com.example.travel.domain.community.entity;

import com.example.travel.domain.community.entity.id.TravelPostTagId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "travel_post_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPostTag {
    @EmbeddedId
    private TravelPostTagId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private TravelPost post;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
