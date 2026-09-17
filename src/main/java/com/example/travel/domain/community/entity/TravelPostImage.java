package com.example.travel.domain.community.entity;

import com.example.travel.global.persistence.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "travel_post_images", uniqueConstraints =
        @UniqueConstraint(name = "uk_post_image_order", columnNames = {"post_id", "display_order"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPostImage extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private TravelPost post;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    private TravelPostImage(TravelPost post, String objectKey, short displayOrder) {
        this.post = post;
        this.objectKey = objectKey;
        this.displayOrder = displayOrder;
    }

    public static TravelPostImage create(TravelPost post, String objectKey, short displayOrder) {
        return new TravelPostImage(post, objectKey, displayOrder);
    }

    public void changeDisplayOrder(short displayOrder) {
        this.displayOrder = displayOrder;
    }

}
