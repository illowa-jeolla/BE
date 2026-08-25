package com.example.travel.domain.job.controller;

import com.example.travel.domain.job.dto.JunnamPublicJobListResponse;
import com.example.travel.domain.job.dto.JunnamPublicJobDetailResponse;
import com.example.travel.domain.job.dto.TourJobDetailResponse;
import com.example.travel.domain.job.dto.TourJobListResponse;
import com.example.travel.domain.job.dto.TourJobSearchCondition;
import com.example.travel.domain.job.service.ExternalJobService;
import com.example.travel.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/external")
public class ExternalJobController {
    private final ExternalJobService externalJobService;

    public ExternalJobController(ExternalJobService externalJobService) {
        this.externalJobService = externalJobService;
    }

    @GetMapping("/tour")
    public ResponseEntity<ApiResponse<TourJobListResponse>> tourJobs(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "12") int numOfRows,
            @RequestParam(defaultValue = "D") String arrange,
            @RequestParam(required = false) String regnCd,
            @RequestParam(required = false) String signguCd,
            @RequestParam(required = false) String wrkpAdresText,
            @RequestParam(required = false) String empmnTitle,
            @RequestParam(required = false) String rcritJssfcCd,
            @RequestParam(required = false) String crrDivCd,
            @RequestParam(required = false) String acdmcrCd,
            @RequestParam(required = false) String salStleCd,
            @RequestParam(required = false) String eplmtStleCd,
            @RequestParam(required = false) String minRegDt,
            @RequestParam(required = false) String maxRegDt,
            @RequestParam(required = false) String minMdfcnDt,
            @RequestParam(required = false) String maxMdfcnDt) {
        TourJobSearchCondition condition = new TourJobSearchCondition(
                pageNo, numOfRows, arrange, regnCd, signguCd, wrkpAdresText, empmnTitle, rcritJssfcCd,
                crrDivCd, acdmcrCd, salStleCd, eplmtStleCd, minRegDt, maxRegDt, minMdfcnDt, maxMdfcnDt);
        return ResponseEntity.ok(ApiResponse.success(externalJobService.findTourJobs(condition)));
    }

    @GetMapping("/tour/jeonnam-gwangju")
    public ResponseEntity<ApiResponse<TourJobListResponse>> jeonnamGwangjuTourJobs(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "12") int numOfRows,
            @RequestParam(defaultValue = "D") String arrange,
            @RequestParam(required = false) String signguCd,
            @RequestParam(required = false) String wrkpAdresText,
            @RequestParam(required = false) String empmnTitle,
            @RequestParam(required = false) String rcritJssfcCd,
            @RequestParam(required = false) String crrDivCd,
            @RequestParam(required = false) String acdmcrCd,
            @RequestParam(required = false) String salStleCd,
            @RequestParam(required = false) String eplmtStleCd,
            @RequestParam(required = false) String minRegDt,
            @RequestParam(required = false) String maxRegDt,
            @RequestParam(required = false) String minMdfcnDt,
            @RequestParam(required = false) String maxMdfcnDt) {
        TourJobSearchCondition condition = new TourJobSearchCondition(
                pageNo, numOfRows, arrange, null, signguCd, wrkpAdresText, empmnTitle, rcritJssfcCd,
                crrDivCd, acdmcrCd, salStleCd, eplmtStleCd, minRegDt, maxRegDt, minMdfcnDt, maxMdfcnDt);
        return ResponseEntity.ok(ApiResponse.success(externalJobService.findJeonnamGwangjuTourJobs(condition)));
    }

    @GetMapping("/tour/{employmentInfoNo}")
    public ResponseEntity<ApiResponse<TourJobDetailResponse>> tourJobDetail(
            @PathVariable String employmentInfoNo) {
        return ResponseEntity.ok(ApiResponse.success(externalJobService.findTourJobDetail(employmentInfoNo)));
    }

    @GetMapping("/junnam")
    public ResponseEntity<ApiResponse<JunnamPublicJobListResponse>> junnamPublicJobs(
            @RequestParam(defaultValue = "1") int startPage,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "12") int numOfRows,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(
                externalJobService.findJunnamPublicJobs(startPage, pageSize, numOfRows, region)));
    }

    @GetMapping("/junnam/{jobKey}")
    public ResponseEntity<ApiResponse<JunnamPublicJobDetailResponse>> junnamPublicJobDetail(
            @PathVariable String jobKey) {
        return ResponseEntity.ok(ApiResponse.success(externalJobService.findJunnamPublicJobDetail(jobKey)));
    }
}
