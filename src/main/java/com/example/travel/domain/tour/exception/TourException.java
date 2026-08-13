package com.example.travel.domain.tour.exception;

import com.example.travel.global.exception.BusinessException;

public class TourException extends BusinessException {
    public TourException(TourErrorCode errorCode) {
        super(errorCode);
    }

    public TourException(TourErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
