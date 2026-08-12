package com.example.travel.domain.auth.google.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google")
public record GoogleProperties(String clientId, String clientSecret, String redirectUri) {}
