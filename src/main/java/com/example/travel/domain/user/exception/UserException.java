package com.example.travel.domain.user.exception;

import com.example.travel.global.exception.BusinessException;

public class UserException extends BusinessException {
    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
