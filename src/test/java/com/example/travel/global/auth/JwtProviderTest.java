package com.example.travel.global.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {
    private final JwtProvider provider = new JwtProvider(new JwtProperties(
            "test-secret-key-must-be-at-least-32-bytes-long", 60_000, 120_000, false));

    @Test
    void accessTokenContainsUserAndRole() {
        String token = provider.createAccessToken(7L, "USER");

        assertThat(provider.isValidAccessToken(token)).isTrue();
        assertThat(provider.isValidRefreshToken(token)).isFalse();
        assertThat(provider.userId(token)).isEqualTo(7L);
        assertThat(provider.role(token)).isEqualTo("USER");
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String token = provider.createRefreshToken(7L);

        assertThat(provider.isValidRefreshToken(token)).isTrue();
        assertThat(provider.isValidAccessToken(token)).isFalse();
    }
}
