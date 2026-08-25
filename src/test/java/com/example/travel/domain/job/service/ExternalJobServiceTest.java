package com.example.travel.domain.job.service;

import com.example.travel.domain.job.client.JunnamPublicJobApiClient;
import com.example.travel.domain.job.client.TourJobApiClient;
import com.example.travel.domain.job.dto.TourJobItem;
import com.example.travel.domain.job.dto.TourJobListResponse;
import com.example.travel.domain.job.dto.TourJobSearchCondition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalJobServiceTest {
    private final TourJobApiClient tourJobApiClient = mock(TourJobApiClient.class);
    private final JunnamPublicJobApiClient junnamPublicJobApiClient = mock(JunnamPublicJobApiClient.class);
    private final ExternalJobService service = new ExternalJobService(tourJobApiClient, junnamPublicJobApiClient);

    @Test
    void findJeonnamGwangjuTourJobsBuildsGlobalPageSortedByArrange() {
        TourJobSearchCondition condition = new TourJobSearchCondition(
                2, 2, "D", null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        when(tourJobApiClient.findJobs(regionCondition("5", 4)))
                .thenReturn(new TourJobListResponse(1, 4, 3, List.of(
                        item("g1", "광주1", "2026-08-24 10:00:00", "2026-08-24 10:00:00"),
                        item("g2", "광주2", "2026-08-22 10:00:00", "2026-08-22 10:00:00"),
                        item("g3", "광주3", "2026-08-20 10:00:00", "2026-08-20 10:00:00"))));
        when(tourJobApiClient.findJobs(regionCondition("38", 4)))
                .thenReturn(new TourJobListResponse(1, 4, 3, List.of(
                        item("j1", "전남1", "2026-08-23 10:00:00", "2026-08-23 10:00:00"),
                        item("j2", "전남2", "2026-08-21 10:00:00", "2026-08-21 10:00:00"),
                        item("j3", "전남3", "2026-08-19 10:00:00", "2026-08-19 10:00:00"))));

        TourJobListResponse response = service.findJeonnamGwangjuTourJobs(condition);

        assertThat(response.pageNo()).isEqualTo(2);
        assertThat(response.numOfRows()).isEqualTo(2);
        assertThat(response.totalCount()).isEqualTo(6);
        assertThat(response.items())
                .extracting(TourJobItem::employmentInfoNo)
                .containsExactly("g2", "j2");

        ArgumentCaptor<TourJobSearchCondition> captor = ArgumentCaptor.forClass(TourJobSearchCondition.class);
        verify(tourJobApiClient, times(2)).findJobs(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(TourJobSearchCondition::pageNo, TourJobSearchCondition::numOfRows,
                        TourJobSearchCondition::regnCd)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1, 4, "5"),
                        org.assertj.core.groups.Tuple.tuple(1, 4, "38"));
    }

    private TourJobSearchCondition regionCondition(String regionCode, int numOfRows) {
        return new TourJobSearchCondition(
                1, numOfRows, "D", regionCode, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    private TourJobItem item(String employmentInfoNo, String title, String registeredAt, String modifiedAt) {
        return new TourJobItem(
                employmentInfoNo,
                null,
                null,
                title,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                modifiedAt,
                registeredAt);
    }
}
