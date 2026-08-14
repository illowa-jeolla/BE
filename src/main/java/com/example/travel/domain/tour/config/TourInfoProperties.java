package com.example.travel.domain.tour.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour-info")
public record TourInfoProperties(
        String serviceKey,
        String baseUrl,
        String mobileOs,
        String mobileApp
) {
    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
