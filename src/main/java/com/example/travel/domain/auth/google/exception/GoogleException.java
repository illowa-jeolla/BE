package com.example.travel.domain.auth.google.exception;

import com.example.travel.global.exception.BusinessException;

public class GoogleException extends BusinessException {
    public GoogleException(GoogleErrorCode errorCode) {
        super(errorCode);
    }

    public GoogleException(GoogleErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
