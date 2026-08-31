package com.example.travel.domain.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "junnam-public-job")
public record JunnamPublicJobApiProperties(
        String serviceKey,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryBackoff
) {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(15);
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_RETRY_BACKOFF = Duration.ofMillis(300);

    public JunnamPublicJobApiProperties(String serviceKey, String baseUrl) {
        this(serviceKey, baseUrl, null, null, 0, null);
    }

    public JunnamPublicJobApiProperties {
        connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
        maxAttempts = maxAttempts < 1 ? DEFAULT_MAX_ATTEMPTS : maxAttempts;
        retryBackoff = retryBackoff == null ? DEFAULT_RETRY_BACKOFF : retryBackoff;
    }

    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
