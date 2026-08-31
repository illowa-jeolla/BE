package com.example.travel.domain.community.dto.response;

public record TravelPostImageResponse(
        Long imageId,
        String objectKey,
        String imageUrl,
        String contentType,
        short displayOrder
) {
}
