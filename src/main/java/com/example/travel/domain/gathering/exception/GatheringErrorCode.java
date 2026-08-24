package com.example.travel.domain.gathering.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GatheringErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "GATHERING_404_USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "GATHERING_404_REGION_NOT_FOUND",
            "사용 가능한 지역을 찾을 수 없습니다."),
    GATHERING_NOT_FOUND(HttpStatus.NOT_FOUND, "GATHERING_404_NOT_FOUND",
            "게더링을 찾을 수 없습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "GATHERING_409_ALREADY_JOINED",
            "이미 참여한 게더링입니다."),
    REJOIN_NOT_ALLOWED(HttpStatus.CONFLICT, "GATHERING_409_REJOIN_NOT_ALLOWED",
            "이미 참여한 게더링은 재참여할 수 없습니다."),
    NOT_OPEN(HttpStatus.CONFLICT, "GATHERING_409_NOT_OPEN",
            "현재 참여할 수 없는 게더링입니다."),
    CAPACITY_FULL(HttpStatus.CONFLICT, "GATHERING_409_CAPACITY_FULL",
            "게더링 정원이 마감되었습니다."),
    ALREADY_STARTED(HttpStatus.CONFLICT, "GATHERING_409_ALREADY_STARTED",
            "이미 시작된 게더링에는 참여할 수 없습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "GATHERING_404_PARTICIPANT_NOT_FOUND",
            "게더링 참여 정보를 찾을 수 없습니다."),
    ALREADY_CANCELLED(HttpStatus.CONFLICT, "GATHERING_409_ALREADY_CANCELLED",
            "이미 취소한 게더링 참여입니다."),
    HOST_CANNOT_CANCEL_PARTICIPATION(HttpStatus.CONFLICT,
            "GATHERING_409_HOST_CANNOT_CANCEL_PARTICIPATION",
            "방장은 게더링 참여를 취소할 수 없습니다."),
    PARTICIPANT_LIST_FORBIDDEN(HttpStatus.FORBIDDEN,
            "GATHERING_403_PARTICIPANT_LIST_FORBIDDEN",
            "게더링 참여자만 참여자 목록을 조회할 수 있습니다."),
    HOST_PERMISSION_REQUIRED(HttpStatus.FORBIDDEN,
            "GATHERING_403_HOST_PERMISSION_REQUIRED",
            "게더링 방장만 수행할 수 있습니다."),
    INVALID_CAPACITY(HttpStatus.BAD_REQUEST,
            "GATHERING_400_INVALID_CAPACITY",
            "정원은 현재 참여 인원보다 작을 수 없습니다."),
    NOT_EDITABLE(HttpStatus.CONFLICT, "GATHERING_409_NOT_EDITABLE",
            "현재 상태에서는 게더링을 수정할 수 없습니다."),
    EMPTY_UPDATE(HttpStatus.BAD_REQUEST, "GATHERING_400_EMPTY_UPDATE",
            "수정할 항목을 하나 이상 입력해야 합니다."),
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
