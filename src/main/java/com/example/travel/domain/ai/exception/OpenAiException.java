package com.example.travel.domain.ai.exception;

import com.example.travel.global.exception.BusinessException;

public class OpenAiException extends BusinessException {
    public OpenAiException(OpenAiErrorCode errorCode) {
        super(errorCode);
    }

    public OpenAiException(OpenAiErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
