package com.example.travel.domain.tour.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TourErrorCode implements ErrorCode {
    MISSING_SERVICE_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "TOUR_500_MISSING_KEY",
            "관광정보 API 키가 설정되어 있지 않습니다."),
    UNKNOWN_REGION(HttpStatus.BAD_REQUEST, "TOUR_400_UNKNOWN_REGION",
            "지원하지 않는 지역입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "TOUR_404_NOT_FOUND",
            "관광지 데이터를 찾을 수 없습니다."),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "TOUR_502_UPSTREAM_ERROR",
            "관광정보 API 응답이 올바르지 않습니다."),
    UNAVAILABLE(HttpStatus.BAD_GATEWAY, "TOUR_502_UNAVAILABLE",
            "관광정보 API에 연결할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    TourErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
