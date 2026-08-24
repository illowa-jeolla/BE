package com.example.travel.domain.location.dto;

import java.math.BigDecimal;

public record LocationSearchItem(
        String kakaoPlaceId,
        String name,
        String category,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer distanceMeters,
        String placeUrl
) {}
