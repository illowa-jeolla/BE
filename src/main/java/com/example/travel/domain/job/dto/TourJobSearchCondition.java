package com.example.travel.domain.job.dto;

public record TourJobSearchCondition(
        int pageNo,
        int numOfRows,
        String arrange,
        String regnCd,
        String signguCd,
        String wrkpAdresText,
        String empmnTitle,
        String rcritJssfcCd,
        String crrDivCd,
        String acdmcrCd,
        String salStleCd,
        String eplmtStleCd,
        String minRegDt,
        String maxRegDt,
        String minMdfcnDt,
        String maxMdfcnDt
) {
}
