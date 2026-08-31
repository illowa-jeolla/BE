package com.example.travel.domain.job.client;

import com.example.travel.domain.job.config.JunnamPublicJobApiProperties;
import com.example.travel.domain.job.dto.JunnamPublicJobDetailResponse;
import com.example.travel.domain.job.dto.JunnamPublicJobListResponse;
import com.example.travel.domain.job.exception.ExternalJobErrorCode;
import com.example.travel.domain.job.exception.ExternalJobException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JunnamPublicJobApiClientTest {
    private final JunnamPublicJobApiProperties properties = new JunnamPublicJobApiProperties(
            "service-key", "https://apis.data.go.kr/6460000/jnGovjobInfo");

    @Test
    void findJobsReturnsXmlItemsAndRawFields() {
        List<URI> requestedUris = new ArrayList<>();
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                    <response>
                      <header>
                        <resultCode>00</resultCode>
                        <resultMsg>NORMAL SERVICE.</resultMsg>
                      </header>
                      <body>
                        <pageIndex>1</pageIndex>
                        <pageSize>12</pageSize>
                        <numOfRows>12</numOfRows>
                        <totalCount>1</totalCount>
                        <items>
                          <item>
                            <corpNm>전남기업</corpNm>
                            <jobTitle>보이는 일자리</jobTitle>
                            <addr>전라남도 목포시</addr>
                            <tel>061-000-0000</tel>
                            <homepage>https://example.com</homepage>
                          </item>
                        </items>
                      </body>
                    </response>
                    """;
        });

        JunnamPublicJobListResponse response = client.findJobs(1, 12, 12);

        assertThat(response.startPage()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(12);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).companyName()).isEqualTo("전남기업");
        assertThat(response.items().get(0).title()).isEqualTo("보이는 일자리");
        assertThat(response.items().get(0).rawFields()).containsEntry("corpNm", "전남기업");
        assertThat(requestedUris.get(0).toString())
                .contains("/getGovjobList?", "pageSize=12", "startPage=1",
                        "numOfRows=12", "ServiceKey=service-key")
                .doesNotContain("/getGovjobList/getGovjobList");
    }

    @Test
    void findJobsPassesRegionAsJobAreaQueryParameter() {
        List<URI> requestedUris = new ArrayList<>();
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                    <response>
                      <header>
                        <resultCode>00</resultCode>
                        <resultMsg>NORMAL SERVICE.</resultMsg>
                      </header>
                      <body>
                        <pageIndex>1</pageIndex>
                        <pageSize>12</pageSize>
                        <numOfRows>12</numOfRows>
                        <totalCount>8</totalCount>
                        <items>
                          <item>
                            <jobKey>1</jobKey>
                            <jobTitle>보성 일자리</jobTitle>
                            <jobCategoryNm>보성</jobCategoryNm>
                          </item>
                        </items>
                      </body>
                    </response>
                    """;
        });

        JunnamPublicJobListResponse response = client.findJobs(1, 12, 12, "보성");

        assertThat(response.totalCount()).isEqualTo(8);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).isEqualTo("보성 일자리");
        assertThat(requestedUris.get(0).toString()).contains("jobArea=%EB%B3%B4%EC%84%B1");
    }

    @Test
    void findJobsDoesNotFilterWhenRegionIsAll() {
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> """
                <response>
                  <header>
                    <resultCode>00</resultCode>
                    <resultMsg>NORMAL SERVICE.</resultMsg>
                  </header>
                  <body>
                    <pageIndex>1</pageIndex>
                    <pageSize>12</pageSize>
                    <numOfRows>12</numOfRows>
                    <totalCount>2</totalCount>
                    <items>
                      <item>
                        <jobKey>1</jobKey>
                        <jobTitle>보성 일자리</jobTitle>
                        <jobCategoryNm>보성</jobCategoryNm>
                      </item>
                      <item>
                        <jobKey>2</jobKey>
                        <jobTitle>나주 일자리</jobTitle>
                        <jobCategoryNm>나주</jobCategoryNm>
                      </item>
                    </items>
                  </body>
                </response>
                """);

        JunnamPublicJobListResponse response = client.findJobs(1, 12, 12, "전체");

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void findJobsDoesNotPassJobAreaWhenRegionIsAllInEnglish() {
        List<URI> requestedUris = new ArrayList<>();
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                    <response>
                      <header>
                        <resultCode>00</resultCode>
                        <resultMsg>NORMAL SERVICE.</resultMsg>
                      </header>
                      <body>
                        <pageIndex>1</pageIndex>
                        <pageSize>12</pageSize>
                        <numOfRows>12</numOfRows>
                        <totalCount>2</totalCount>
                        <items>
                          <item>
                            <jobKey>1</jobKey>
                            <jobTitle>보성 일자리</jobTitle>
                            <jobCategoryNm>보성</jobCategoryNm>
                          </item>
                          <item>
                            <jobKey>2</jobKey>
                            <jobTitle>나주 일자리</jobTitle>
                            <jobCategoryNm>나주</jobCategoryNm>
                          </item>
                        </items>
                      </body>
                    </response>
                    """;
        });

        JunnamPublicJobListResponse response = client.findJobs(1, 12, 12, "all");

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);
        assertThat(requestedUris.get(0).toString()).doesNotContain("jobArea=");
    }

    @Test
    void throwsUpstreamErrorWhenOpenApiErrorResponseComes() {
        int[] attempts = {0};
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            attempts[0]++;
            return """
                    <OpenAPI_ServiceResponse>
                      <cmmMsgHeader>
                        <errMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</errMsg>
                        <returnAuthMsg>등록되지 않은 서비스키</returnAuthMsg>
                      </cmmMsgHeader>
                    </OpenAPI_ServiceResponse>
                    """;
        });

        assertThatThrownBy(() -> client.findJobs(1, 12, 12))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.UPSTREAM_ERROR.code());
        assertThat(attempts[0]).isOne();
    }

    @Test
    void findJobDetailReturnsXmlItemAndRawFields() {
        List<URI> requestedUris = new ArrayList<>();
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                    <response>
                      <header>
                        <resultCode>00</resultCode>
                        <resultMsg>NORMAL SERVICE.</resultMsg>
                      </header>
                      <body>
                        <items>
                          <item>
                            <jobKey>10675</jobKey>
                            <jobTitle>2026년 시니어의사 기간제근로자 채용 공고</jobTitle>
                            <jobWriter>관리자</jobWriter>
                            <jobCategoryNm>나주</jobCategoryNm>
                            <jobInsertDt>2026-08-24</jobInsertDt>
                            <jobReadCnt>0</jobReadCnt>
                            <jobContent>상세 내용</jobContent>
                          </item>
                        </items>
                      </body>
                    </response>
                    """;
        });

        JunnamPublicJobDetailResponse response = client.findJobDetail("10675");

        assertThat(response.jobKey()).isEqualTo("10675");
        assertThat(response.title()).isEqualTo("2026년 시니어의사 기간제근로자 채용 공고");
        assertThat(response.writer()).isEqualTo("관리자");
        assertThat(response.content()).isEqualTo("상세 내용");
        assertThat(response.rawFields()).containsEntry("jobCategoryNm", "나주");
        assertThat(requestedUris.get(0).toString())
                .contains("/getGovjobInfo?", "jobKey=10675", "ServiceKey=service-key")
                .doesNotContain("/getGovjobInfo/getGovjobInfo");
    }

    @Test
    void findJobDetailThrowsWhenItemIsMissing() {
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> """
                <response>
                  <header>
                    <resultCode>00</resultCode>
                    <resultMsg>NORMAL SERVICE.</resultMsg>
                  </header>
                  <body/>
                </response>
                """);

        assertThatThrownBy(() -> client.findJobDetail("missing"))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.NOT_FOUND.code());
    }

    @Test
    void throwsMissingServiceKeyWhenJunnamKeyIsBlank() {
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(
                new JunnamPublicJobApiProperties("", "https://apis.data.go.kr/6460000/jnGovjobInfo"),
                uri -> "");

        assertThatThrownBy(() -> client.findJobs(1, 12, 12))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.MISSING_JUNNAM_SERVICE_KEY.code());
    }

    @Test
    void httpErrorBodyIsHandledAsUpstreamError() {
        int[] attempts = {0};
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            attempts[0]++;
            throw new RestClientResponseException(
                    "Internal Server Error",
                    500,
                    "Internal Server Error",
                    HttpHeaders.EMPTY,
                    """
                    <OpenAPI_ServiceResponse>
                      <cmmMsgHeader>
                        <errMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</errMsg>
                        <returnAuthMsg>등록되지 않은 서비스키</returnAuthMsg>
                      </cmmMsgHeader>
                    </OpenAPI_ServiceResponse>
                    """.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
        });

        assertThatThrownBy(() -> client.findJobs(1, 12, 12))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.UPSTREAM_ERROR.code());
        assertThat(attempts[0]).isOne();
    }

    @Test
    void httpErrorWithUnrecognizedBodyIsHandledAsUpstreamError() {
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            throw new RestClientResponseException(
                    "Internal Server Error",
                    500,
                    "Internal Server Error",
                    HttpHeaders.EMPTY,
                    """
                    <response>
                      <body/>
                    </response>
                    """.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
        });

        assertThatThrownBy(() -> client.findJobs(1, 12, 12))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.UPSTREAM_ERROR.code());
    }

    @Test
    void throwsUpstreamErrorWhenSuccessResultCodeIsMissing() {
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> """
                <response>
                  <body>
                    <items/>
                  </body>
                </response>
                """);

        assertThatThrownBy(() -> client.findJobs(1, 12, 12))
                .isInstanceOf(ExternalJobException.class)
                .extracting("code")
                .isEqualTo(ExternalJobErrorCode.UPSTREAM_ERROR.code());
    }

    @Test
    void findJobsRetriesTransientRequestFailure() {
        int[] attempts = {0};
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            attempts[0]++;
            if (attempts[0] == 1) {
                throw new ResourceAccessException("Read timed out");
            }
            return """
                    <response>
                      <header>
                        <resultCode>00</resultCode>
                        <resultMsg>NORMAL SERVICE.</resultMsg>
                      </header>
                      <body>
                        <pageIndex>1</pageIndex>
                        <pageSize>12</pageSize>
                        <numOfRows>12</numOfRows>
                        <totalCount>1</totalCount>
                        <items>
                          <item>
                            <jobKey>1</jobKey>
                            <jobTitle>재시도 성공 일자리</jobTitle>
                          </item>
                        </items>
                      </body>
                    </response>
                    """;
        });

        JunnamPublicJobListResponse response = client.findJobs(1, 12, 12);

        assertThat(attempts[0]).isEqualTo(2);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).isEqualTo("재시도 성공 일자리");
    }

    @Test
    void findJobsRetriesMalformedTransientBody() {
        int[] attempts = {0};
        JunnamPublicJobApiClient client = new JunnamPublicJobApiClient(properties, uri -> {
            attempts[0]++;
            if (attempts[0] == 1) {
                return "<html>upstream gateway error</html>";
            }
            return """
                    <response>
                      <header>
                        <resultCode>00</resultCode>
                        <resultMsg>NORMAL SERVICE.</resultMsg>
                      </header>
                      <body>
                        <pageIndex>1</pageIndex>
                        <pageSize>12</pageSize>
                        <numOfRows>12</numOfRows>
                        <totalCount>0</totalCount>
                        <items/>
                      </body>
                    </response>
                    """;
        });

        JunnamPublicJobListResponse response = client.findJobs(1, 12, 12);

        assertThat(attempts[0]).isEqualTo(2);
        assertThat(response.totalCount()).isZero();
    }
}
