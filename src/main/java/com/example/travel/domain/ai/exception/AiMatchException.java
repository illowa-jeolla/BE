package com.example.travel.domain.ai.exception;

import com.example.travel.global.exception.BusinessException;

public class AiMatchException extends BusinessException {
    public AiMatchException(AiMatchErrorCode errorCode) { super(errorCode); }
    public AiMatchException(AiMatchErrorCode errorCode, Throwable cause) { super(errorCode, cause); }
}
