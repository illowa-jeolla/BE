package com.example.travel.domain.tour.dto;

import java.math.BigDecimal;

public record TourPlaceItem(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        String thumbnailUrl,
        BigDecimal mapX,
        BigDecimal mapY
) {}
