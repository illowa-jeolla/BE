package com.example.travel.domain.job.exception;

import com.example.travel.global.exception.BusinessException;

public class ExternalJobException extends BusinessException {
    public ExternalJobException(ExternalJobErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalJobException(ExternalJobErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
