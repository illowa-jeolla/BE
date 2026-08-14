package com.example.travel.domain.tour.client;

import com.example.travel.domain.tour.config.TourInfoProperties;
import com.example.travel.domain.tour.dto.TourPlaceDetailResponse;
import com.example.travel.domain.tour.dto.TourPlaceItem;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import com.example.travel.domain.tour.exception.TourErrorCode;
import com.example.travel.domain.tour.exception.TourException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class TourInfoClient {
    private static final int MAX_RADIUS = 20_000;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(7);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TourInfoProperties properties;
    private final Function<URI, String> bodyFetcher;

    @Autowired
    public TourInfoClient(TourInfoProperties properties) {
        this(properties, createBodyFetcher());
    }

    TourInfoClient(TourInfoProperties properties, Function<URI, String> bodyFetcher) {
        this.properties = properties;
        this.bodyFetcher = bodyFetcher;
    }

    public TourPlaceMapResponse findPlaces(String regionName, int pageNo, int numOfRows) {
        validateServiceKey();

        TourRegion region = TourRegion.find(regionName)
                .orElseThrow(() -> new TourException(TourErrorCode.UNKNOWN_REGION));

        JsonNode response = request(locationBasedListUri(region, pageNo, numOfRows));
        validateResponse(response);

        JsonNode body = response.path("response").path("body");
        return new TourPlaceMapResponse(
                intValue(body, "pageNo", pageNo),
                intValue(body, "numOfRows", numOfRows),
                intValue(body, "totalCount", 0),
                places(body.path("items").path("item")));
    }

    public TourPlaceDetailResponse findPlaceDetail(String contentId) {
        validateServiceKey();

        JsonNode response = request(detailCommonUri(contentId));
        validateResponse(response);

        List<JsonNode> items = items(response.path("response").path("body").path("items").path("item"));
        if (items.isEmpty()) {
            throw new TourException(TourErrorCode.NOT_FOUND);
        }
        return detail(items.get(0));
    }

    private void validateServiceKey() {
        if (!properties.hasServiceKey()) {
            throw new TourException(TourErrorCode.MISSING_SERVICE_KEY);
        }
    }

    private JsonNode request(URI uri) {
        try {
            String body = bodyFetcher.apply(uri);
            if (body == null || body.isBlank()) throw tourUnavailable();
            return OBJECT_MAPPER.readTree(body);
        } catch (JsonProcessingException exception) {
            throw new TourException(TourErrorCode.UPSTREAM_ERROR, exception);
        } catch (RestClientException exception) {
            throw tourUnavailable(exception);
        }
    }

    private void validateResponse(JsonNode response) {
        JsonNode header = response.path("response").path("header");
        String resultCode = text(header, "resultCode");
        if (!"0000".equals(resultCode)) {
            throw new TourException(TourErrorCode.UPSTREAM_ERROR);
        }
    }

    private URI locationBasedListUri(TourRegion region, int pageNo, int numOfRows) {
        String encodedServiceKey = serviceKeyForQuery();
        UriComponentsBuilder builder = commonUriBuilder("/locationBasedList2")
                .queryParam("arrange", "S")
                .queryParam("contentTypeId", "12")
                .queryParam("mapX", region.longitude())
                .queryParam("mapY", region.latitude())
                .queryParam("radius", MAX_RADIUS)
                .queryParam("pageNo", Math.max(pageNo, 1))
                .queryParam("numOfRows", Math.min(Math.max(numOfRows, 1), 30));
        return uriWithServiceKey(builder, encodedServiceKey);
    }

    private URI detailCommonUri(String contentId) {
        UriComponentsBuilder builder = commonUriBuilder("/detailCommon2")
                .queryParam("contentId", contentId);
        return uriWithServiceKey(builder, serviceKeyForQuery());
    }

    private UriComponentsBuilder commonUriBuilder(String path) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(path)
                .queryParam("MobileOS", properties.mobileOs())
                .queryParam("MobileApp", properties.mobileApp())
                .queryParam("_type", "json");
    }

    private URI uriWithServiceKey(UriComponentsBuilder builder, String encodedServiceKey) {
        String uri = builder
                .build()
                .encode()
                .toUriString();
        return URI.create(uri + "&serviceKey=" + encodedServiceKey);
    }

    private String serviceKeyForQuery() {
        String key = properties.serviceKey().trim();
        if (key.contains("%")) return key;
        return URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    private List<TourPlaceItem> places(JsonNode itemsNode) {
        List<TourPlaceItem> places = new ArrayList<>();
        for (JsonNode item : items(itemsNode)) {
            places.add(place(item));
        }
        return places;
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

    private TourPlaceItem place(JsonNode item) {
        return new TourPlaceItem(
                text(item, "contentid"),
                text(item, "contenttypeid"),
                text(item, "title"),
                address(item),
                text(item, "firstimage2"),
                decimal(item, "mapx"),
                decimal(item, "mapy"));
    }

    private TourPlaceDetailResponse detail(JsonNode item) {
        return new TourPlaceDetailResponse(
                text(item, "contentid"),
                text(item, "contenttypeid"),
                text(item, "title"),
                address(item),
                text(item, "tel"),
                text(item, "homepage"),
                text(item, "overview"),
                text(item, "firstimage"),
                text(item, "firstimage2"),
                decimal(item, "mapx"),
                decimal(item, "mapy"),
                text(item, "zipcode"));
    }

    private String address(JsonNode item) {
        String addr1 = text(item, "addr1");
        String addr2 = text(item, "addr2");
        if (addr2 == null || addr2.isBlank()) return addr1;
        if (addr1 == null || addr1.isBlank()) return addr2;
        return addr1 + " " + addr2;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int intValue(JsonNode node, String field, int fallback) {
        JsonNode value = node.path(field);
        return value.isNumber() || value.isTextual() ? value.asInt(fallback) : fallback;
    }

    private TourException tourUnavailable() {
        return new TourException(TourErrorCode.UNAVAILABLE);
    }

    private TourException tourUnavailable(Throwable cause) {
        return new TourException(TourErrorCode.UNAVAILABLE, cause);
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
