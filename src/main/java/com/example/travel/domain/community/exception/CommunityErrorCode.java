package com.example.travel.domain.community.exception;

import com.example.travel.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CommunityErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_404_USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다."),
    DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_404_DRAFT_NOT_FOUND",
            "작성 중인 임시 글을 찾을 수 없습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_404_IMAGE_NOT_FOUND",
            "이미지를 찾을 수 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_404_POST_NOT_FOUND",
            "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_404_COMMENT_NOT_FOUND",
            "댓글을 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_404_REGION_NOT_FOUND",
            "사용 가능한 지역을 찾을 수 없습니다."),
    EMPTY_IMAGE(HttpStatus.BAD_REQUEST, "COMMUNITY_400_EMPTY_IMAGE",
            "빈 이미지 파일은 업로드할 수 없습니다."),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "COMMUNITY_400_IMAGE_TOO_LARGE",
            "이미지 파일은 10MB 이하여야 합니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "COMMUNITY_400_UNSUPPORTED_IMAGE_TYPE",
            "JPEG, PNG, WEBP 이미지만 업로드할 수 있습니다."),
    INVALID_IMAGE_SIGNATURE(HttpStatus.BAD_REQUEST, "COMMUNITY_400_INVALID_IMAGE_SIGNATURE",
            "이미지 파일의 실제 형식이 올바르지 않습니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMUNITY_400_IMAGE_LIMIT_EXCEEDED",
            "게시글에는 이미지를 최대 5장까지 첨부할 수 있습니다."),
    INVALID_IMAGE_ORDER(HttpStatus.BAD_REQUEST, "COMMUNITY_400_INVALID_IMAGE_ORDER",
            "이미지 순서 정보가 올바르지 않습니다."),
    PUBLISH_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "COMMUNITY_400_PUBLISH_TITLE_REQUIRED",
            "게시하려면 제목을 입력해야 합니다."),
    PUBLISH_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "COMMUNITY_400_PUBLISH_CONTENT_REQUIRED",
            "게시하려면 본문을 입력해야 합니다."),
    PUBLISH_REGION_REQUIRED(HttpStatus.BAD_REQUEST, "COMMUNITY_400_PUBLISH_REGION_REQUIRED",
            "게시하려면 지역을 선택해야 합니다."),
    POST_PERMISSION_REQUIRED(HttpStatus.FORBIDDEN, "COMMUNITY_403_POST_PERMISSION_REQUIRED",
            "게시글 작성자만 수행할 수 있습니다."),
    COMMENT_PERMISSION_REQUIRED(HttpStatus.FORBIDDEN,
            "COMMUNITY_403_COMMENT_PERMISSION_REQUIRED",
            "댓글 작성자만 수행할 수 있습니다."),
    IMAGE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COMMUNITY_500_IMAGE_READ_FAILED",
            "이미지 파일을 읽지 못했습니다."),
    IMAGE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COMMUNITY_500_IMAGE_STORAGE_FAILED",
            "이미지 파일을 저장하지 못했습니다."),
    IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COMMUNITY_500_IMAGE_DELETE_FAILED",
            "이미지 파일을 삭제하지 못했습니다."),
    INVALID_IMAGE_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "COMMUNITY_500_INVALID_IMAGE_KEY",
            "이미지 저장 경로가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CommunityErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
