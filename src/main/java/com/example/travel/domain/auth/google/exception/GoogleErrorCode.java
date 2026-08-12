package com.example.travel.domain.auth.google.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GoogleErrorCode implements ErrorCode {
    MISSING_CODE(HttpStatus.BAD_REQUEST, "GOOGLE_400_MISSING_CODE",
            "Google 인가 코드가 없습니다."),
    INVALID_STATE(HttpStatus.UNAUTHORIZED, "GOOGLE_401_INVALID_STATE",
            "유효하지 않은 Google 로그인 요청입니다."),
    LOGIN_CANCELLED(HttpStatus.UNAUTHORIZED, "GOOGLE_401_LOGIN_CANCELLED",
            "Google 로그인이 취소되었습니다."),
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "GOOGLE_401_INVALID_ID_TOKEN",
            "유효하지 않은 Google ID Token입니다."),
    EMAIL_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "GOOGLE_422_EMAIL_REQUIRED",
            "Google 계정의 인증된 이메일이 필요합니다."),
    API_ERROR(HttpStatus.BAD_GATEWAY, "GOOGLE_502_API_ERROR",
            "Google 로그인 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    GoogleErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
