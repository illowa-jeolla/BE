package com.example.travel.domain.ai.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AiMatchErrorCode implements ErrorCode {
    INVALID_PRIORITIES(HttpStatus.BAD_REQUEST, "AI_MATCH_400_INVALID_PRIORITIES", "생활 우선순위를 중복 없이 모두 입력해야 합니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_MATCH_404_REGION", "관심 지역을 찾을 수 없습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_MATCH_404_REQUEST", "AI 매칭 요청을 찾을 수 없습니다."),
    RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_MATCH_404_RESULT", "AI 매칭 결과를 찾을 수 없습니다."),
    CACHE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_MATCH_503_CACHE", "AI 매칭 임시 저장소를 사용할 수 없습니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI_MATCH_429_DAILY_LIMIT", "AI 매칭은 하루에 최대 2번 요청할 수 있습니다."),
    CANDIDATE_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "AI_MATCH_422_CANDIDATE", "추천 가능한 일자리 또는 관광지가 부족합니다."),
    INVALID_AI_RESPONSE(HttpStatus.BAD_GATEWAY, "AI_MATCH_502_INVALID_RESPONSE", "AI 추천 결과를 검증할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AiMatchErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String message() { return message; }
}
