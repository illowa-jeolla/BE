package com.example.travel.domain.community.exception;

import com.example.travel.global.exception.BusinessException;

public class CommunityException extends BusinessException {
    public CommunityException(CommunityErrorCode errorCode) {
        super(errorCode);
    }
}
