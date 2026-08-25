package com.example.travel.domain.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "junnam-public-job")
public record JunnamPublicJobApiProperties(
        String serviceKey,
        String baseUrl
) {
    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
