package com.example.travel.domain.travel.dto.response;

import java.math.BigDecimal;

public record ManualTravelPlaceItem(
        String contentId,
        String title,
        String address,
        String thumbnailUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer distanceMeters
) {}
