package com.example.travel.domain.job.client;

import com.example.travel.domain.job.config.TourJobApiProperties;
import com.example.travel.domain.job.dto.TourJobDetailResponse;
import com.example.travel.domain.job.dto.TourJobListResponse;
import com.example.travel.domain.job.dto.TourJobSearchCondition;
import com.example.travel.domain.job.exception.ExternalJobErrorCode;
import com.example.travel.domain.job.exception.ExternalJobException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourJobApiClientTest {
    private final TourJobApiProperties properties = new TourJobApiProperties(
            "service-key", "https://apis.data.go.kr/B551011/tursmService", "ETC", "tour_gong");

    @Test
    void findJobsReturnsListFields() {
        List<URI> requestedUris = new ArrayList<>();
        TourJobApiClient client = new TourJobApiClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                    {
                      "response": {
                        "header": { "resultCode": "0000" },
                        "body": {
                          "pageNo": 1,
                          "numOfRows": 12,
                          "totalCount": 2,
                          "items": {
                            "item": [
                              {
                                "empmnInfoNo": "sisaykorea_1",
                                "corpoNm": "시사와이비엠",
                                "corpoLogoFileUrl": "https://example.com/logo.png",
                                "empmnTtl": "관광 통역 채용",
                                "wrkpAdres": "전라남도 여수시",
                                "regnCd": "38",
                                "signguCd": "13",
                                "salStleCd": "JC0602",
                                "wageAmt": "25000000",
                                "ordtmEmpmnYn": "N",
                                "rcptDdlnDe": "20260831",
                                "crrDivCd": "JC0101",
                                "acdmcrCd": "JC1807",
                                "eplmtStleN1Cd": "JC0202",
                                "mdfcnDt": "2026-08-13 00:00:00",
                                "regDt": "2026-08-13 08:13:55"
                              }
                            ]
                          }
                        }
                      }
                    }
                    """;
        });

        TourJobListResponse response = client.findJobs(condition());

        assertThat(response.pageNo()).isEqualTo(1);
        assertThat(response.numOfRows()).isEqualTo(12);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).employmentInfoNo()).isEqualTo("sisaykorea_1");
        assertThat(response.items().get(0).companyName()).isEqualTo("시사와이비엠");
        assertThat(response.items().get(0).title()).isEqualTo("관광 통역 채용");
        assertThat(response.items().get(0).workplaceAddress()).isEqualTo("전라남도 여수시");
        assertThat(requestedUris.get(0).toString())
                .contains("/empmnInfoList?", "MobileOS=ETC", "MobileApp=tour_gong",
                        "_type=json", "pageNo=1", "numOfRows=12", "arrange=D",
                        "regnCd=38", "empmnTitle=%EA%B4%80%EA%B4%91")
                .doesNotContain("/empmnInfoList/empmnInfoList");
    }

    @Test
    void findJobDetailReturnsDetailFields() {
        List<URI> requestedUris = new ArrayList<>();
        TourJobApiClient client = new TourJobApiClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                    {
                      "response": {
                        "header": { "resultCode": "0000" },
                        "body": {
                          "items": {
                            "item": {
                              "empmnInfoNo": "sisaykorea_1",
                              "empmnTtl": "관광 통역 채용",
                              "dtyCn": "관광 통역 안내",
                              "wageAmt": "25000000",
                              "wrkpAdres": "전라남도 여수시",
                              "wrkpDtadr": "중앙로 1",
                              "empmnChrgrNm": "홍길동",
                              "empmnChrgrTelno": "061-000-0000",
                              "coIntroCn": "회사 소개",
                              "tursmEmpmnInfoURL": "https://example.com/jobs/1"
                            }
                          }
                        }
                      }
                    }
                    """;
        });

        TourJobDetailResponse detail = client.findJobDetail("sisaykorea_1");

        assertThat(detail.employmentInfoNo()).isEqualTo("sisaykorea_1");
        assertThat(detail.title()).isEqualTo("관광 통역 채용");
        assertThat(detail.dutyContent()).isEqualTo("관광 통역 안내");
        assertThat(detail.workplaceAddress()).isEqualTo("전라남도 여수시");
        assertThat(detail.managerName()).isEqualTo("홍길동");
        assertThat(detail.detailUrl()).isEqualTo("https://example.com/jobs/1");
        assertThat(requestedUris.get(0).toString())
                .contains("/empmnInfoDetail?", "empmnInfoNo=sisaykorea_1");
    }

    @Test
    void findJobDetailThrowsWhenItemIsMissing() {
        TourJobApiClient client = new TourJobApiClient(properties, uri -> """
                {
                  "response": {
                    "header": { "resultCode": "0000" },
                    "body": { "items": "" }
                  }
                }
                """);

        assertThatThrownBy(() -> client.findJobDetail("missing"))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.NOT_FOUND.code());
    }

    @Test
    void throwsMissingServiceKeyWhenTourJobKeyIsBlank() {
        TourJobApiClient client = new TourJobApiClient(
                new TourJobApiProperties("", "https://apis.data.go.kr/B551011/tursmService", "ETC", "tour_gong"),
                uri -> "{}");

        assertThatThrownBy(() -> client.findJobs(condition()))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.MISSING_TOUR_JOB_SERVICE_KEY.code());
    }

    @Test
    void httpErrorBodyIsHandledAsUpstreamError() {
        TourJobApiClient client = new TourJobApiClient(properties, uri -> {
            throw new RestClientResponseException(
                    "Bad Gateway",
                    502,
                    "Bad Gateway",
                    HttpHeaders.EMPTY,
                    """
                    {
                      "response": {
                        "header": {
                          "resultCode": "99",
                          "resultMsg": "INVALID REQUEST"
                        }
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
        });

        assertThatThrownBy(() -> client.findJobs(condition()))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.UPSTREAM_ERROR.code());
    }

    private TourJobSearchCondition condition() {
        return new TourJobSearchCondition(
                1, 12, "D", "38", null, null, "관광", null,
                null, null, null, null, null, null, null, null);
    }
}
