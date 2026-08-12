package com.example.travel.domain.auth.kakao.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum KakaoErrorCode implements ErrorCode {
    MISSING_CODE(HttpStatus.BAD_REQUEST, "KAKAO_400_MISSING_CODE",
            "카카오 인가 코드가 없습니다."),
    INVALID_STATE(HttpStatus.UNAUTHORIZED, "KAKAO_401_INVALID_STATE",
            "유효하지 않은 카카오 로그인 요청입니다."),
    LOGIN_CANCELLED(HttpStatus.UNAUTHORIZED, "KAKAO_401_LOGIN_CANCELLED",
            "카카오 로그인이 취소되었습니다."),
    EMAIL_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "KAKAO_422_EMAIL_REQUIRED",
            "카카오 계정의 인증된 이메일 제공 동의가 필요합니다."),
    API_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_502_API_ERROR",
            "카카오 로그인 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    KakaoErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
