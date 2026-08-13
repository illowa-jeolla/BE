 package com.example.travel.global.common;

public record ApiResponse<T>(boolean success, String code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "200", "요청에 성공했습니다.", data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "200", message, data);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
