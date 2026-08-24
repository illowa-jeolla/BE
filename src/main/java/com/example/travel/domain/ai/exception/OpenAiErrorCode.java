package com.example.travel.domain.ai.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OpenAiErrorCode implements ErrorCode {
    NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "OPENAI_503_NOT_CONFIGURED", "AI 추천을 사용할 수 없습니다."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "OPENAI_429_RATE_LIMITED", "AI 요청이 일시적으로 제한되었습니다."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "OPENAI_504_TIMEOUT", "AI 응답 시간이 초과되었습니다."),
    API_ERROR(HttpStatus.BAD_GATEWAY, "OPENAI_502_API_ERROR", "AI 서비스 호출에 실패했습니다."),
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "OPENAI_502_INVALID_RESPONSE", "AI 응답 형식이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    OpenAiErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
