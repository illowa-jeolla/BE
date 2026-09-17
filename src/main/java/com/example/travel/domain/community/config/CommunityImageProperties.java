package com.example.travel.domain.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "community.image")
public record CommunityImageProperties(
        String bucket,
        String region,
        long presignedUrlExpirationMinutes,
        long maxFileSize,
        int maxImagesPerPost
) {
}
