package com.example.travel.domain.job.service;

import com.example.travel.domain.job.client.JunnamPublicJobApiClient;
import com.example.travel.domain.job.client.TourJobApiClient;
import com.example.travel.domain.job.dto.JunnamPublicJobDetailResponse;
import com.example.travel.domain.job.dto.JunnamPublicJobListResponse;
import com.example.travel.domain.job.dto.TourJobDetailResponse;
import com.example.travel.domain.job.dto.TourJobItem;
import com.example.travel.domain.job.dto.TourJobListResponse;
import com.example.travel.domain.job.dto.TourJobSearchCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExternalJobService {
    private static final String GWANGJU_REGION_CODE = "5";
    private static final String JEONNAM_REGION_CODE = "38";

    private final TourJobApiClient tourJobApiClient;
    private final JunnamPublicJobApiClient junnamPublicJobApiClient;

    public ExternalJobService(TourJobApiClient tourJobApiClient,
                              JunnamPublicJobApiClient junnamPublicJobApiClient) {
        this.tourJobApiClient = tourJobApiClient;
        this.junnamPublicJobApiClient = junnamPublicJobApiClient;
    }

    public TourJobListResponse findTourJobs(TourJobSearchCondition condition) {
        return tourJobApiClient.findJobs(condition);
    }

    public TourJobListResponse findJeonnamGwangjuTourJobs(TourJobSearchCondition condition) {
        TourJobListResponse gwangju = tourJobApiClient.findJobs(withRegionCode(condition, GWANGJU_REGION_CODE));
        TourJobListResponse jeonnam = tourJobApiClient.findJobs(withRegionCode(condition, JEONNAM_REGION_CODE));

        List<TourJobItem> items = new ArrayList<>();
        items.addAll(gwangju.items());
        items.addAll(jeonnam.items());
        int numOfRows = Math.min(Math.max(condition.numOfRows(), 1), 100);
        if (items.size() > numOfRows) {
            items = items.subList(0, numOfRows);
        }

        return new TourJobListResponse(
                Math.max(condition.pageNo(), 1),
                numOfRows,
                gwangju.totalCount() + jeonnam.totalCount(),
                items);
    }

    public TourJobDetailResponse findTourJobDetail(String employmentInfoNo) {
        return tourJobApiClient.findJobDetail(employmentInfoNo);
    }

    public JunnamPublicJobListResponse findJunnamPublicJobs(int startPage, int pageSize, int numOfRows) {
        return findJunnamPublicJobs(startPage, pageSize, numOfRows, null);
    }

    public JunnamPublicJobListResponse findJunnamPublicJobs(int startPage, int pageSize, int numOfRows,
                                                            String region) {
        return junnamPublicJobApiClient.findJobs(startPage, pageSize, numOfRows, region);
    }

    public JunnamPublicJobDetailResponse findJunnamPublicJobDetail(String jobKey) {
        return junnamPublicJobApiClient.findJobDetail(jobKey);
    }

    private TourJobSearchCondition withRegionCode(TourJobSearchCondition condition, String regionCode) {
        return new TourJobSearchCondition(
                condition.pageNo(),
                condition.numOfRows(),
                condition.arrange(),
                regionCode,
                condition.signguCd(),
                condition.wrkpAdresText(),
                condition.empmnTitle(),
                condition.rcritJssfcCd(),
                condition.crrDivCd(),
                condition.acdmcrCd(),
                condition.salStleCd(),
                condition.eplmtStleCd(),
                condition.minRegDt(),
                condition.maxRegDt(),
                condition.minMdfcnDt(),
                condition.maxMdfcnDt());
    }
}
