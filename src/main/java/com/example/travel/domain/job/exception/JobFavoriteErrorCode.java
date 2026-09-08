package com.example.travel.domain.job.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum JobFavoriteErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "JOB_FAVORITE_404_USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "JOB_FAVORITE_404_NOT_FOUND", "찜한 일자리를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    JobFavoriteErrorCode(HttpStatus status, String code, String message) {
        this.status = status; this.code = code; this.message = message;
    }
    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
