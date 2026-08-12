package com.example.travel.domain.auth.google;

import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleUserInfoTest {
    @Test
    void normalizesOnlyVerifiedEmail() {
        var verified = new GoogleUserInfo(
                "subject", " USER@EXAMPLE.COM ", true, "사용자", null);
        var unverified = new GoogleUserInfo(
                "subject", "user@example.com", false, "사용자", null);

        assertThat(verified.verifiedEmail()).isEqualTo("user@example.com");
        assertThat(unverified.verifiedEmail()).isNull();
    }

    @Test
    void usesFallbackAndTruncatesNickname() {
        assertThat(new GoogleUserInfo("subject", "user@example.com", true, null, null)
                .nicknameOrDefault()).isEqualTo("구글사용자");
        assertThat(new GoogleUserInfo("subject", "user@example.com", true,
                "가".repeat(51), null).nicknameOrDefault()).hasSize(50);
    }
}
