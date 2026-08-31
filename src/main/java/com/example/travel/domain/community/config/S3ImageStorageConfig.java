package com.example.travel.domain.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "community.image.storage", havingValue = "s3", matchIfMissing = true)
public class S3ImageStorageConfig {
    @Bean
    S3Client s3Client(CommunityImageProperties properties) {
        return S3Client.builder().region(Region.of(properties.region())).build();
    }

    @Bean
    S3Presigner s3Presigner(CommunityImageProperties properties) {
        return S3Presigner.builder().region(Region.of(properties.region())).build();
    }
}
