package com.example.travel.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(
        URI oauthCallbackUri,
        String origin
) {}
