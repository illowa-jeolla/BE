package com.example.travel.domain.tour.dto;

import java.math.BigDecimal;

public record TourPlaceDetailResponse(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        String tel,
        String homepage,
        String overview,
        String firstImage,
        String firstImageThumbnail,
        BigDecimal mapX,
        BigDecimal mapY,
        String zipcode
) {}
