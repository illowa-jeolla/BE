package com.example.travel.domain.travel.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TravelRecommendationErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL_404_USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL_404_REGION_NOT_FOUND", "여행 지역을 찾을 수 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "TRAVEL_400_INVALID_DATE_RANGE", "여행 종료일은 출발일보다 빠를 수 없습니다."),
    PAST_START_DATE(HttpStatus.BAD_REQUEST, "TRAVEL_400_PAST_START_DATE", "여행 출발일은 오늘보다 빠를 수 없습니다."),
    TRIP_TOO_LONG(HttpStatus.BAD_REQUEST, "TRAVEL_400_TRIP_TOO_LONG", "여행 기간은 최대 7일까지 선택할 수 있습니다."),
    DAILY_PLACE_COUNTS_MISMATCH(HttpStatus.BAD_REQUEST, "TRAVEL_400_DAILY_PLACE_COUNTS_MISMATCH", "일자별 관광지 개수는 여행 일수만큼 입력해야 합니다."),
    LODGING_OUTSIDE_REGION(HttpStatus.BAD_REQUEST, "TRAVEL_400_LODGING_OUTSIDE_REGION", "선택한 숙소가 여행 지역의 검색 범위를 벗어났습니다."),
    NO_CANDIDATES(HttpStatus.UNPROCESSABLE_ENTITY, "TRAVEL_422_NO_CANDIDATES", "숙소 주변에서 추천 가능한 관광지를 찾지 못했습니다."),
    INSUFFICIENT_CANDIDATES(HttpStatus.UNPROCESSABLE_ENTITY, "TRAVEL_422_INSUFFICIENT_CANDIDATES", "요청한 관광지 개수보다 추천 가능한 후보가 적습니다."),
    CANDIDATE_CACHE_NOT_FOUND(HttpStatus.GONE, "TRAVEL_410_CANDIDATE_CACHE_NOT_FOUND", "추천 후보 정보가 만료되었습니다."),
    CANDIDATE_CACHE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "TRAVEL_503_CANDIDATE_CACHE_UNAVAILABLE", "추천 후보 저장소를 사용할 수 없습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL_404_REQUEST_NOT_FOUND", "여행 추천 요청을 찾을 수 없습니다."),
    GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL_404_GUIDE_NOT_FOUND", "여행 가이드를 찾을 수 없습니다."),
    DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL_404_DRAFT_NOT_FOUND", "임시 여행 가이드를 찾을 수 없거나 보관 기간이 만료되었습니다."),
    SAVED_GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL_404_SAVED_GUIDE_NOT_FOUND", "저장된 여행 가이드를 찾을 수 없습니다."),
    GUIDE_ALREADY_SAVED(HttpStatus.CONFLICT, "TRAVEL_409_GUIDE_ALREADY_SAVED", "이미 저장된 여행 가이드입니다."),
    GUIDE_ALREADY_DELETED(HttpStatus.CONFLICT, "TRAVEL_409_GUIDE_ALREADY_DELETED", "이미 저장 취소된 여행 가이드입니다."),
    REFRESH_ALREADY_USED(HttpStatus.CONFLICT, "TRAVEL_409_REFRESH_ALREADY_USED", "새로 추천받기를 이미 사용했습니다."),
    REFRESH_INSUFFICIENT_CANDIDATES(HttpStatus.UNPROCESSABLE_ENTITY, "TRAVEL_422_REFRESH_INSUFFICIENT_CANDIDATES", "이전 관광지를 제외하면 새로 추천할 관광지가 부족합니다."),
    INVALID_MANUAL_DAYS(HttpStatus.BAD_REQUEST, "TRAVEL_400_INVALID_MANUAL_DAYS", "수동 일정의 날짜 구성이 여행 기간과 일치하지 않습니다."),
    MANUAL_PLACE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "TRAVEL_400_MANUAL_PLACE_COUNT_EXCEEDED", "날짜별 관광지 선택 개수를 초과했습니다."),
    DUPLICATE_MANUAL_PLACE(HttpStatus.BAD_REQUEST, "TRAVEL_400_DUPLICATE_MANUAL_PLACE", "같은 관광지를 중복 선택할 수 없습니다."),
    MANUAL_PLACE_OUTSIDE_SEARCH_RADIUS(HttpStatus.BAD_REQUEST, "TRAVEL_400_MANUAL_PLACE_OUTSIDE_SEARCH_RADIUS", "숙소 반경 20km 밖의 관광지는 선택할 수 없습니다."),
    INVALID_AI_PLACE_ID(HttpStatus.BAD_GATEWAY, "TRAVEL_502_INVALID_AI_PLACE_ID", "AI가 유효하지 않은 관광지를 반환했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    TravelRecommendationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return code; }
    @Override public String message() { return message; }
}
