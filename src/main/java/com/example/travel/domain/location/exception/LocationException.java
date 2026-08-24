package com.example.travel.domain.location.exception;

import com.example.travel.global.exception.BusinessException;

public class LocationException extends BusinessException {
    public LocationException(LocationErrorCode errorCode) {
        super(errorCode);
    }

    public LocationException(LocationErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
