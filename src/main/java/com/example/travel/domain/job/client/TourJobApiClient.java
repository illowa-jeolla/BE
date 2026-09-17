package com.example.travel.domain.job.client;

import com.example.travel.domain.job.config.TourJobApiProperties;
import com.example.travel.domain.job.dto.TourJobDetailResponse;
import com.example.travel.domain.job.dto.TourJobItem;
import com.example.travel.domain.job.dto.TourJobListResponse;
import com.example.travel.domain.job.dto.TourJobSearchCondition;
import com.example.travel.domain.job.exception.ExternalJobErrorCode;
import com.example.travel.domain.job.exception.ExternalJobException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class TourJobApiClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(7);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TourJobApiProperties properties;
    private final Function<URI, String> bodyFetcher;

    @Autowired
    public TourJobApiClient(TourJobApiProperties properties) {
        this(properties, createBodyFetcher());
    }

    TourJobApiClient(TourJobApiProperties properties, Function<URI, String> bodyFetcher) {
        this.properties = properties;
        this.bodyFetcher = bodyFetcher;
    }

    public TourJobListResponse findJobs(TourJobSearchCondition condition) {
        validateServiceKey();

        JsonNode response = request(listUri(condition));
        validateResponse(response);

        JsonNode body = response.path("response").path("body");
        return new TourJobListResponse(
                intValue(body, "pageNo", Math.max(condition.pageNo(), 1)),
                intValue(body, "numOfRows", clampRows(condition.numOfRows())),
                intValue(body, "totalCount", 0),
                jobs(body.path("items").path("item")));
    }

    public TourJobDetailResponse findJobDetail(String employmentInfoNo) {
        validateServiceKey();

        JsonNode response = request(detailUri(employmentInfoNo));
        validateResponse(response);

        List<JsonNode> items = items(response.path("response").path("body").path("items").path("item"));
        if (items.isEmpty()) {
            throw new ExternalJobException(ExternalJobErrorCode.NOT_FOUND);
        }
        return detail(items.get(0));
    }

    private void validateServiceKey() {
        if (!properties.hasServiceKey()) {
            throw new ExternalJobException(ExternalJobErrorCode.MISSING_TOUR_JOB_SERVICE_KEY);
        }
    }

    private JsonNode request(URI uri) {
        try {
            String body = bodyFetcher.apply(uri);
            if (body == null || body.isBlank()) throw unavailable();
            return OBJECT_MAPPER.readTree(body);
        } catch (RestClientResponseException exception) {
            String body = exception.getResponseBodyAsString();
            if (body == null || body.isBlank()) throw unavailable(exception);
            try {
                return OBJECT_MAPPER.readTree(body);
            } catch (JsonProcessingException jsonException) {
                throw new ExternalJobException(ExternalJobErrorCode.UPSTREAM_ERROR, jsonException);
            }
        } catch (JsonProcessingException exception) {
            throw new ExternalJobException(ExternalJobErrorCode.UPSTREAM_ERROR, exception);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private void validateResponse(JsonNode response) {
        JsonNode header = response.path("response").path("header");
        String resultCode = text(header, "resultCode");
        if (!"0000".equals(resultCode)) {
            throw new ExternalJobException(ExternalJobErrorCode.UPSTREAM_ERROR);
        }
    }

    private URI listUri(TourJobSearchCondition condition) {
        UriComponentsBuilder builder = commonUriBuilder("/empmnInfoList")
                .queryParam("pageNo", Math.max(condition.pageNo(), 1))
                .queryParam("numOfRows", clampRows(condition.numOfRows()))
                .queryParam("arrange", defaultText(condition.arrange(), "D"));
        addOptionalParams(builder, condition);
        return uriWithServiceKey(builder);
    }

    private URI detailUri(String employmentInfoNo) {
        UriComponentsBuilder builder = commonUriBuilder("/empmnInfoDetail")
                .queryParam("empmnInfoNo", employmentInfoNo);
        return uriWithServiceKey(builder);
    }

    private UriComponentsBuilder commonUriBuilder(String path) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(path)
                .queryParam("MobileOS", properties.mobileOs())
                .queryParam("MobileApp", properties.mobileApp())
                .queryParam("_type", "json");
    }

    private void addOptionalParams(UriComponentsBuilder builder, TourJobSearchCondition condition) {
        queryParamIfPresent(builder, "regnCd", condition.regnCd());
        queryParamIfPresent(builder, "signguCd", condition.signguCd());
        queryParamIfPresent(builder, "wrkpAdresText", condition.wrkpAdresText());
        queryParamIfPresent(builder, "empmnTitle", condition.empmnTitle());
        queryParamIfPresent(builder, "rcritJssfcCd", condition.rcritJssfcCd());
        queryParamIfPresent(builder, "crrDivCd", condition.crrDivCd());
        queryParamIfPresent(builder, "acdmcrCd", condition.acdmcrCd());
        queryParamIfPresent(builder, "salStleCd", condition.salStleCd());
        queryParamIfPresent(builder, "eplmtStleCd", condition.eplmtStleCd());
        queryParamIfPresent(builder, "minRegDt", condition.minRegDt());
        queryParamIfPresent(builder, "maxRegDt", condition.maxRegDt());
        queryParamIfPresent(builder, "minMdfcnDt", condition.minMdfcnDt());
        queryParamIfPresent(builder, "maxMdfcnDt", condition.maxMdfcnDt());
    }

    private void queryParamIfPresent(UriComponentsBuilder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(name, value);
        }
    }

    private URI uriWithServiceKey(UriComponentsBuilder builder) {
        String uri = builder.build().encode().toUriString();
        return URI.create(uri + "&serviceKey=" + serviceKeyForQuery());
    }

    private String serviceKeyForQuery() {
        String key = properties.serviceKey().trim();
        if (key.contains("%")) return key;
        return URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    private List<TourJobItem> jobs(JsonNode itemsNode) {
        List<TourJobItem> jobs = new ArrayList<>();
        for (JsonNode item : items(itemsNode)) {
            jobs.add(job(item));
        }
        return jobs;
    }

    private List<JsonNode> items(JsonNode itemsNode) {
        List<JsonNode> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            itemsNode.forEach(items::add);
        } else if (itemsNode.isObject()) {
            items.add(itemsNode);
        }
        return items;
    }

    private TourJobItem job(JsonNode item) {
        return new TourJobItem(
                text(item, "empmnInfoNo"),
                text(item, "corpoNm"),
                text(item, "corpoLogoFileUrl"),
                text(item, "empmnTtl"),
                text(item, "uprRcritJssfcCd"),
                text(item, "midRcritJssfcCd"),
                text(item, "lwprtRcritJssfcCd"),
                text(item, "wrkpAdres"),
                text(item, "regnCd"),
                text(item, "signguCd"),
                text(item, "salStleCd"),
                text(item, "wageAmt"),
                text(item, "ordtmEmpmnYn"),
                text(item, "rcptDdlnDe"),
                text(item, "crrDivCd"),
                text(item, "acdmcrCd"),
                text(item, "eplmtStleN1Cd"),
                text(item, "eplmtStleN2Cd"),
                text(item, "mdfcnDt"),
                text(item, "regDt"));
    }

    private TourJobDetailResponse detail(JsonNode item) {
        return new TourJobDetailResponse(
                text(item, "empmnInfoNo"),
                text(item, "empmnTtl"),
                text(item, "dtyCn"),
                text(item, "rcritPnum"),
                text(item, "salStleCd"),
                text(item, "wageAmt"),
                text(item, "boamtInclsYn"),
                text(item, "boamtRate"),
                text(item, "crrDivCd"),
                text(item, "crrBgngMths"),
                text(item, "crrEndMths"),
                text(item, "acdmcrCd"),
                text(item, "eplmtStleN1Cd"),
                text(item, "eplmtStleN2Cd"),
                text(item, "labrTimeCn"),
                text(item, "ordtmEmpmnYn"),
                text(item, "rcptDdlnDe"),
                text(item, "forLangLevel"),
                text(item, "majorNm"),
                text(item, "qlfcLcnsCn"),
                text(item, "msvcExpt"),
                text(item, "cmputrPlueAbltyCn"),
                text(item, "pvltrt"),
                text(item, "etcPfrtMtrCn"),
                text(item, "dspsnEmpmnHopeCd"),
                text(item, "dspsnPfrtYn"),
                text(item, "stcsMthCn"),
                text(item, "receptMth"),
                text(item, "etcRcptMthDesc"),
                text(item, "sbmsnDocuCn"),
                text(item, "sbmsnDocuFormAtchFileUrl"),
                text(item, "wrkpZpcd"),
                text(item, "wrkpAdres"),
                text(item, "wrkpDtadr"),
                text(item, "wrkStleCn"),
                text(item, "wrkTimeCn"),
                text(item, "feinsr"),
                text(item, "rtrpayDivNm"),
                text(item, "wlfareEtcCn"),
                text(item, "empmnChrgrNm"),
                text(item, "deptNm"),
                text(item, "empmnChrgrTelno"),
                text(item, "empmnChrgrFxnum"),
                text(item, "labrrNumInfo"),
                text(item, "caplAmt"),
                text(item, "ansleAmt"),
                text(item, "entrpsDivNm"),
                text(item, "corpoAdres"),
                text(item, "prmryBsnssCn"),
                text(item, "coIntroCn"),
                text(item, "tursmEmpmnInfoURL"));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private int intValue(JsonNode node, String field, int fallback) {
        JsonNode value = node.path(field);
        return value.isNumber() || value.isTextual() ? value.asInt(fallback) : fallback;
    }

    private int clampRows(int numOfRows) {
        return Math.min(Math.max(numOfRows, 1), 100);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private ExternalJobException unavailable() {
        return new ExternalJobException(ExternalJobErrorCode.UNAVAILABLE);
    }

    private ExternalJobException unavailable(Throwable cause) {
        return new ExternalJobException(ExternalJobErrorCode.UNAVAILABLE, cause);
    }

    private static Function<URI, String> createBodyFetcher() {
        RestClient restClient = createRestClient();
        return uri -> restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }

    private static RestClient createRestClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
