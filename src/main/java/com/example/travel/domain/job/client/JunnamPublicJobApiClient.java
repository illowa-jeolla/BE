package com.example.travel.domain.job.client;

import com.example.travel.domain.job.config.JunnamPublicJobApiProperties;
import com.example.travel.domain.job.dto.JunnamPublicJobDetailResponse;
import com.example.travel.domain.job.dto.JunnamPublicJobItem;
import com.example.travel.domain.job.dto.JunnamPublicJobListResponse;
import com.example.travel.domain.job.exception.ExternalJobErrorCode;
import com.example.travel.domain.job.exception.ExternalJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JunnamPublicJobApiClient {
    private final JunnamPublicJobApiProperties properties;
    private final Function<URI, String> bodyFetcher;

    @Autowired
    public JunnamPublicJobApiClient(JunnamPublicJobApiProperties properties) {
        this(properties, createBodyFetcher(properties));
    }

    JunnamPublicJobApiClient(JunnamPublicJobApiProperties properties, Function<URI, String> bodyFetcher) {
        this.properties = properties;
        this.bodyFetcher = bodyFetcher;
    }

    public JunnamPublicJobListResponse findJobs(int startPage, int pageSize, int numOfRows) {
        return findJobs(startPage, pageSize, numOfRows, null);
    }

    public JunnamPublicJobListResponse findJobs(int startPage, int pageSize, int numOfRows, String region) {
        validateServiceKey();

        ApiResponseDocument response = request(listUri(startPage, pageSize, numOfRows, region));

        Element body = firstElement(response.document(), "body");
        List<JunnamPublicJobItem> items = jobs(response.document());
        if (!isAllRegion(region)) {
            items = filterByRegion(items, region);
        }
        return new JunnamPublicJobListResponse(
                Math.max(startPage, 1),
                intText(body, "pageSize", clampRows(pageSize)),
                intText(body, "numOfRows", clampRows(numOfRows)),
                intText(body, "totalCount", 0),
                items);
    }

    public JunnamPublicJobDetailResponse findJobDetail(String jobKey) {
        validateServiceKey();

        ApiResponseDocument response = request(detailUri(jobKey));

        Element item = firstElement(response.document(), "item");
        if (item == null) {
            throw new ExternalJobException(ExternalJobErrorCode.NOT_FOUND);
        }
        return detail(item);
    }

    private void validateServiceKey() {
        if (!properties.hasServiceKey()) {
            throw new ExternalJobException(ExternalJobErrorCode.MISSING_JUNNAM_SERVICE_KEY);
        }
    }

    private ApiResponseDocument request(URI uri) {
        ExternalJobException lastException = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            ApiResponseDocument response;
            try {
                response = fetchDocument(uri);
            } catch (ExternalJobException exception) {
                lastException = exception;
                if (!isRetryableRequestFailure(exception) || attempt == properties.maxAttempts()) {
                    throw exception;
                }
                sleepBeforeRetry();
                continue;
            }

            if (hasOpenApiAuthError(response)) {
                validateResponse(response);
            }
            if (isRetryableInvalidResponse(response) && attempt < properties.maxAttempts()) {
                sleepBeforeRetry();
                continue;
            }
            validateResponse(response);
            return response;
        }
        throw lastException == null ? unavailable() : lastException;
    }

    private ApiResponseDocument fetchDocument(URI uri) {
        try {
            String body = bodyFetcher.apply(uri);
            if (body == null || body.isBlank()) throw unavailable();
            return new ApiResponseDocument(parseXml(body), false);
        } catch (RestClientResponseException exception) {
            String body = exception.getResponseBodyAsString();
            if (body == null || body.isBlank()) throw unavailable(exception);
            return new ApiResponseDocument(parseXml(body), true);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private boolean isRetryableRequestFailure(ExternalJobException exception) {
        return ExternalJobErrorCode.UNAVAILABLE.code().equals(exception.getCode())
                || ExternalJobErrorCode.UPSTREAM_ERROR.code().equals(exception.getCode());
    }

    private boolean isRetryableInvalidResponse(ApiResponseDocument response) {
        return !hasOpenApiAuthError(response) && !isSuccessResponse(response);
    }

    private boolean isSuccessResponse(ApiResponseDocument response) {
        return !response.httpError() && "00".equals(firstText(response.document(), "resultCode"));
    }

    private boolean hasOpenApiAuthError(ApiResponseDocument response) {
        String openApiError = firstText(response.document(), "returnAuthMsg");
        return openApiError != null && !openApiError.isBlank();
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(properties.retryBackoff().toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw unavailable(interruptedException);
        }
    }

    private Document parseXml(String body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            document.getDocumentElement().normalize();
            return document;
        } catch (Exception exception) {
            throw new ExternalJobException(ExternalJobErrorCode.UPSTREAM_ERROR, exception);
        }
    }

    private void validateResponse(ApiResponseDocument response) {
        String openApiError = firstText(response.document(), "returnAuthMsg");
        if (openApiError != null && !openApiError.isBlank()) {
            throw new ExternalJobException(ExternalJobErrorCode.UPSTREAM_ERROR);
        }

        String resultCode = firstText(response.document(), "resultCode");
        if (response.httpError() || !"00".equals(resultCode)) {
            throw new ExternalJobException(ExternalJobErrorCode.UPSTREAM_ERROR);
        }
    }

    private record ApiResponseDocument(Document document, boolean httpError) {
    }

    private URI listUri(int startPage, int pageSize, int numOfRows, String region) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/getGovjobList")
                .queryParam("pageSize", clampRows(pageSize))
                .queryParam("startPage", Math.max(startPage, 1))
                .queryParam("numOfRows", clampRows(numOfRows));
        if (!isAllRegion(region)) {
            builder.queryParam("jobArea", region.trim());
        }
        String uri = builder.build().encode().toUriString();
        return URI.create(uri + "&ServiceKey=" + serviceKeyForQuery());
    }

    private URI detailUri(String jobKey) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/getGovjobInfo")
                .queryParam("jobKey", jobKey);
        String uri = builder.build().encode().toUriString();
        return URI.create(uri + "&ServiceKey=" + serviceKeyForQuery());
    }

    private String serviceKeyForQuery() {
        String key = properties.serviceKey().trim();
        if (key.contains("%")) return key;
        return URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    private List<JunnamPublicJobItem> jobs(Document document) {
        List<JunnamPublicJobItem> jobs = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("item");
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element item) {
                Map<String, String> fields = fields(item);
                jobs.add(new JunnamPublicJobItem(
                        firstPresent(fields, "corpNm", "corpoNm", "companyName", "entNm", "cpnNm"),
                        firstPresent(fields, "jobTitle", "title", "empmnTtl", "ttl", "spcNm"),
                        firstPresent(fields, "addr", "address", "wrkpAdres", "roadNmAddr", "adres"),
                        firstPresent(fields, "tel", "phone", "telNo", "empmnChrgrTelno"),
                        firstPresent(fields, "homepage", "homepageUrl", "url", "linkUrl"),
                        fields));
            }
        }
        return jobs;
    }

    private List<JunnamPublicJobItem> filterByRegion(List<JunnamPublicJobItem> items, String region) {
        String requestedRegion = region.trim();
        return items.stream()
                .filter(item -> requestedRegion.equals(item.rawFields().get("jobCategoryNm")))
                .toList();
    }

    private boolean isAllRegion(String region) {
        return region == null
                || region.isBlank()
                || "전체".equals(region.trim())
                || "all".equalsIgnoreCase(region.trim());
    }

    private JunnamPublicJobDetailResponse detail(Element item) {
        Map<String, String> fields = fields(item);
        return new JunnamPublicJobDetailResponse(
                firstPresent(fields, "jobKey", "id", "seq", "nttId"),
                firstPresent(fields, "jobTitle", "title", "ttl", "subject"),
                firstPresent(fields, "jobWriter", "writer", "regNm", "author"),
                firstPresent(fields, "jobCategoryNm", "categoryName", "category", "regionNm"),
                firstPresent(fields, "jobInsertDt", "insertDt", "regDt", "createdAt"),
                firstPresent(fields, "jobReadCnt", "readCnt", "viewCount"),
                firstPresent(fields, "jobContent", "content", "cn", "detail", "body"),
                firstPresent(fields, "addr", "address", "roadNmAddr", "adres"),
                firstPresent(fields, "tel", "phone", "telNo"),
                firstPresent(fields, "homepage", "homepageUrl", "url", "linkUrl"),
                fields);
    }

    private Map<String, String> fields(Element item) {
        Map<String, String> fields = new LinkedHashMap<>();
        NodeList children = item.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element) {
                fields.put(element.getTagName(), element.getTextContent());
            }
        }
        return fields;
    }

    private String firstPresent(Map<String, String> fields, String... names) {
        for (String name : names) {
            String value = fields.get(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Element firstElement(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 || !(nodes.item(0) instanceof Element element) ? null : element;
    }

    private String firstText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private int intText(Element parent, String tagName, int fallback) {
        if (parent == null) return fallback;
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return fallback;
        try {
            return Integer.parseInt(nodes.item(0).getTextContent());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private int clampRows(int rows) {
        return Math.min(Math.max(rows, 1), 100);
    }

    private ExternalJobException unavailable() {
        return new ExternalJobException(ExternalJobErrorCode.UNAVAILABLE);
    }

    private ExternalJobException unavailable(Throwable cause) {
        return new ExternalJobException(ExternalJobErrorCode.UNAVAILABLE, cause);
    }

    private static Function<URI, String> createBodyFetcher(JunnamPublicJobApiProperties properties) {
        RestClient restClient = createRestClient(properties);
        return uri -> {
            byte[] body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);
            return body == null ? null : new String(body, StandardCharsets.UTF_8);
        };
    }

    private static RestClient createRestClient(JunnamPublicJobApiProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
