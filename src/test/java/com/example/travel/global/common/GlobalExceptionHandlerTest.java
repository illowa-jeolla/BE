package com.example.travel.global.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsCommonResponseForUnsupportedMediaType() {
        var response = handler.handleUnsupportedMediaType();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isEqualTo(ApiResponse.fail(
                "COMMON_415_UNSUPPORTED_MEDIA_TYPE",
                "지원하지 않는 Content-Type입니다. 이미지 업로드는 multipart/form-data를 사용해야 합니다."));
    }
}
