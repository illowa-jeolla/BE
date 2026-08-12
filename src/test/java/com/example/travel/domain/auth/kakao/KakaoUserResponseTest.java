package com.example.travel.domain.auth.kakao;

import com.example.travel.domain.auth.kakao.dto.KakaoUserResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoUserResponseTest {
    @Test
    void usesFallbackWhenNicknameIsNotProvided() {
        assertThat(new KakaoUserResponse(1L, null).nicknameOrDefault())
                .isEqualTo("카카오사용자");
    }

    @Test
    void truncatesNicknameToUserColumnLength() {
        String longNickname = "가".repeat(51);
        var response = new KakaoUserResponse(1L, new KakaoUserResponse.KakaoAccount(
                true, false, true, true, "USER@EXAMPLE.COM",
                new KakaoUserResponse.Profile(longNickname, null)));

        assertThat(response.nicknameOrDefault()).hasSize(50);
    }

    @Test
    void returnsNormalizedEmailOnlyWhenVerified() {
        var verified = new KakaoUserResponse(1L, new KakaoUserResponse.KakaoAccount(
                true, false, true, true, " USER@EXAMPLE.COM ", null));
        var unverified = new KakaoUserResponse(1L, new KakaoUserResponse.KakaoAccount(
                true, false, true, false, "user@example.com", null));

        assertThat(verified.verifiedEmail()).contains("user@example.com");
        assertThat(unverified.verifiedEmail()).isEmpty();
    }

    @Test
    void returnsTrimmedProfileImageUrlWhenProvided() {
        var response = new KakaoUserResponse(1L, new KakaoUserResponse.KakaoAccount(
                true, false, true, true, "user@example.com",
                new KakaoUserResponse.Profile("사용자", " https://example.com/kakao.jpg ")));

        assertThat(response.profileImageUrl()).contains("https://example.com/kakao.jpg");
    }
}
