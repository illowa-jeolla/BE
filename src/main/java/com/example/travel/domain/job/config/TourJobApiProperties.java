package com.example.travel.domain.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour-job")
public record TourJobApiProperties(
        String serviceKey,
        String baseUrl,
        String mobileOs,
        String mobileApp
) {
    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
