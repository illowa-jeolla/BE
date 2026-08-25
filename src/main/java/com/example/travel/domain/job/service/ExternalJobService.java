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
import java.util.Comparator;
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
        int pageNo = Math.max(condition.pageNo(), 1);
        int numOfRows = Math.min(Math.max(condition.numOfRows(), 1), 100);
        int requiredRows = pageNo * numOfRows;

        TourJobListResponse gwangju = collectRegionJobs(condition, GWANGJU_REGION_CODE, requiredRows);
        TourJobListResponse jeonnam = collectRegionJobs(condition, JEONNAM_REGION_CODE, requiredRows);

        List<TourJobItem> items = new ArrayList<>();
        items.addAll(gwangju.items());
        items.addAll(jeonnam.items());
        items.sort(comparator(condition.arrange()));

        int fromIndex = Math.min((pageNo - 1) * numOfRows, items.size());
        int toIndex = Math.min(fromIndex + numOfRows, items.size());

        return new TourJobListResponse(
                pageNo,
                numOfRows,
                gwangju.totalCount() + jeonnam.totalCount(),
                items.subList(fromIndex, toIndex));
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

    private TourJobListResponse collectRegionJobs(TourJobSearchCondition condition, String regionCode,
                                                  int requiredRows) {
        int numOfRows = Math.min(requiredRows, 100);
        TourJobListResponse firstPage = tourJobApiClient.findJobs(withPageAndRegionCode(condition, 1, numOfRows,
                regionCode));
        List<TourJobItem> items = new ArrayList<>(firstPage.items());

        int pageNo = 2;
        while (items.size() < Math.min(firstPage.totalCount(), requiredRows)) {
            TourJobListResponse page = tourJobApiClient.findJobs(withPageAndRegionCode(condition, pageNo, numOfRows,
                    regionCode));
            if (page.items().isEmpty()) {
                break;
            }
            items.addAll(page.items());
            pageNo++;
        }

        return new TourJobListResponse(1, numOfRows, firstPage.totalCount(), items);
    }

    private TourJobSearchCondition withPageAndRegionCode(TourJobSearchCondition condition, int pageNo, int numOfRows,
                                                         String regionCode) {
        return new TourJobSearchCondition(
                pageNo,
                numOfRows,
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

    private Comparator<TourJobItem> comparator(String arrange) {
        if ("A".equalsIgnoreCase(arrange)) {
            return Comparator.comparing(TourJobItem::title, Comparator.nullsLast(String::compareTo));
        }
        if ("C".equalsIgnoreCase(arrange)) {
            return Comparator.comparing(TourJobItem::modifiedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(TourJobItem::registeredAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
