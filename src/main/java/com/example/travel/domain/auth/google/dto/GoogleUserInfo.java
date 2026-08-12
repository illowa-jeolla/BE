package com.example.travel.domain.auth.google.dto;

import java.util.Locale;

public record GoogleUserInfo(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String picture
) {
    public String verifiedEmail() {
        if (!emailVerified || email == null || email.isBlank()) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public String nicknameOrDefault() {
        String nickname = name == null || name.isBlank() ? "구글사용자" : name.trim();
        return nickname.substring(0, Math.min(nickname.length(), 50));
    }
}
