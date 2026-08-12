package com.example.travel.domain.auth.kakao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(String clientId, String clientSecret, String redirectUri) {}
