package com.example.travel.domain.auth.kakao.exception;

import com.example.travel.global.exception.BusinessException;

public class KakaoException extends BusinessException {
    public KakaoException(KakaoErrorCode errorCode) {
        super(errorCode);
    }

    public KakaoException(KakaoErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
