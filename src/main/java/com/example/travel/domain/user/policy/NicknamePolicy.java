package com.example.travel.domain.user.policy;

public final class NicknamePolicy {
    public static final int MAX_LENGTH = 50;

    private NicknamePolicy() {}

    public static String truncate(String value) {
        if (value.length() <= MAX_LENGTH) return value;

        int endIndex = MAX_LENGTH;
        if (Character.isHighSurrogate(value.charAt(endIndex - 1))
                && Character.isLowSurrogate(value.charAt(endIndex))) {
            endIndex--;
        }
        return value.substring(0, endIndex);
    }
}
