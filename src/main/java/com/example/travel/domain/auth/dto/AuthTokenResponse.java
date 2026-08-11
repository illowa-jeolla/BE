package com.example.travel.domain.auth.dto;

public record AuthTokenResponse(String accessToken, String tokenType) {
    public static AuthTokenResponse bearer(String accessToken) {
        return new AuthTokenResponse(accessToken, "Bearer");
    }
}
