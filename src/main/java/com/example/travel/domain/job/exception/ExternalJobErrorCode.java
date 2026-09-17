package com.example.travel.domain.job.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ExternalJobErrorCode implements ErrorCode {
    MISSING_TOUR_JOB_SERVICE_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "JOB_500_MISSING_TOUR_JOB_KEY",
            "관광 일자리 API 키가 설정되어 있지 않습니다."),
    MISSING_JUNNAM_SERVICE_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "JOB_500_MISSING_JUNNAM_KEY",
            "전남 공공 일자리 API 키가 설정되어 있지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "JOB_404_NOT_FOUND",
            "일자리 데이터를 찾을 수 없습니다."),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "JOB_502_UPSTREAM_ERROR",
            "일자리 API 응답이 올바르지 않습니다."),
    UNAVAILABLE(HttpStatus.BAD_GATEWAY, "JOB_502_UNAVAILABLE",
            "일자리 API에 연결할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ExternalJobErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
