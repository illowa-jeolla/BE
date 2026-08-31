package com.example.travel.domain.community.dto.response;

import com.example.travel.domain.community.entity.TravelPostImage;

public record DraftImageItem(Long imageId, String imageUrl, short displayOrder) {
    public static DraftImageItem from(TravelPostImage image, String imageUrl) {
        return new DraftImageItem(image.getId(), imageUrl, image.getDisplayOrder());
    }
}
