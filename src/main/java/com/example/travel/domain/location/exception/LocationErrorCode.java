package com.example.travel.domain.location.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum LocationErrorCode implements ErrorCode {
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOCATION_404_REGION_NOT_FOUND",
            "사용 가능한 지역을 찾을 수 없습니다."),
    REGION_COORDINATES_MISSING(HttpStatus.UNPROCESSABLE_CONTENT,
            "LOCATION_422_REGION_COORDINATES_MISSING",
            "선택한 지역의 중심 좌표가 등록되어 있지 않습니다."),
    MISSING_API_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "LOCATION_500_MISSING_KEY",
            "카카오 지도 API 키가 설정되어 있지 않습니다."),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "LOCATION_502_UPSTREAM_ERROR",
            "장소 검색 응답이 올바르지 않습니다."),
    UNAVAILABLE(HttpStatus.BAD_GATEWAY, "LOCATION_502_UNAVAILABLE",
            "장소 검색 서비스에 연결할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    LocationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
