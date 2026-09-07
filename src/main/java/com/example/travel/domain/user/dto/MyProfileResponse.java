package com.example.travel.domain.user.dto;

import com.example.travel.domain.user.entity.User;

public record MyProfileResponse(Long userId, String email, String nickname, String avatarUrl) {
    public static MyProfileResponse from(User user, String email) {
        return new MyProfileResponse(user.getId(), email, user.getNickname(), user.getAvatarUrl());
    }
}
