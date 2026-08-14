package com.example.travel.domain.gathering.exception;

import com.example.travel.global.exception.BusinessException;

public class GatheringException extends BusinessException {
    public GatheringException(GatheringErrorCode errorCode) {
        super(errorCode);
    }
}
