package com.example.travel.domain.auth.kakao.dto;

import com.example.travel.domain.user.policy.NicknamePolicy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount account) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            @JsonProperty("has_email") Boolean hasEmail,
            @JsonProperty("email_needs_agreement") Boolean emailNeedsAgreement,
            @JsonProperty("is_email_valid") Boolean emailValid,
            @JsonProperty("is_email_verified") Boolean emailVerified,
            String email,
            Profile profile
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
    ) {}

    public String nicknameOrDefault() {
        if (account == null || account.profile() == null
                || account.profile().nickname() == null || account.profile().nickname().isBlank()) {
            return "카카오사용자";
        }
        return NicknamePolicy.truncate(account.profile().nickname());
    }

    public Optional<String> verifiedEmail() {
        if (account == null
                || !Boolean.TRUE.equals(account.hasEmail())
                || Boolean.TRUE.equals(account.emailNeedsAgreement())
                || !Boolean.TRUE.equals(account.emailValid())
                || !Boolean.TRUE.equals(account.emailVerified())
                || account.email() == null
                || account.email().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(account.email().trim().toLowerCase());
    }

    public Optional<String> profileImageUrl() {
        if (account == null || account.profile() == null
                || account.profile().profileImageUrl() == null
                || account.profile().profileImageUrl().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(account.profile().profileImageUrl().trim());
    }
}
