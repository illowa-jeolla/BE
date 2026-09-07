package com.example.travel.domain.job.exception;

import com.example.travel.global.exception.BusinessException;

public class JobFavoriteException extends BusinessException {
    public JobFavoriteException(JobFavoriteErrorCode errorCode) { super(errorCode); }
}
