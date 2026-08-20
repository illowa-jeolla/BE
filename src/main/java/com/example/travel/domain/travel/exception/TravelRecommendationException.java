package com.example.travel.domain.travel.exception;

import com.example.travel.global.exception.BusinessException;

public class TravelRecommendationException extends BusinessException {
    public TravelRecommendationException(TravelRecommendationErrorCode errorCode) {
        super(errorCode);
    }

    public TravelRecommendationException(TravelRecommendationErrorCode errorCode,
                                         Throwable cause) {
        super(errorCode, cause);
    }
}
