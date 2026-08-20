package com.example.travel.domain.location.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao-map")
public record KakaoMapProperties(String restApiKey, String baseUrl) {
    public boolean hasRestApiKey() {
        return restApiKey != null && !restApiKey.isBlank();
    }
}
