package com.example.travel.domain.job.exception;

import com.example.travel.global.exception.BusinessException;

public class JobApplicationException extends BusinessException {
    public JobApplicationException(JobApplicationErrorCode errorCode) {
        super(errorCode);
    }
}
