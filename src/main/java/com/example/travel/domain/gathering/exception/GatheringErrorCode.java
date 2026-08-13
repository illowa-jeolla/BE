package com.example.travel.domain.gathering.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GatheringErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "GATHERING_404_USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "GATHERING_404_REGION_NOT_FOUND",
            "사용 가능한 지역을 찾을 수 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "GATHERING_400_INVALID_DATE_RANGE",
            "검색 종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_START_TIME(HttpStatus.BAD_REQUEST, "GATHERING_400_INVALID_START_TIME",
            "날짜와 시간은 현재보다 미래여야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    GatheringErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
