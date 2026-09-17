package com.example.travel.domain.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "community.image.local")
public record LocalImageProperties(String directory, String baseUrl) {
}
