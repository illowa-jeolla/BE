package com.example.travel.domain.tour.dto;

import java.math.BigDecimal;

public record TourPlaceItem(
        String contentId,
        String contentTypeId,
        String category1,
        String category2,
        String category3,
        String title,
        String address,
        String thumbnailUrl,
        BigDecimal mapX,
        BigDecimal mapY,
        Integer distanceMeters
) {}
