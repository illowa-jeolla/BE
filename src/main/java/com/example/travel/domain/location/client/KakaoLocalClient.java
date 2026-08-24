package com.example.travel.domain.location.client;

import com.example.travel.domain.location.config.KakaoMapProperties;
import com.example.travel.domain.location.dto.LocationSearchItem;
import com.example.travel.domain.location.dto.LocationSearchResponse;
import com.example.travel.domain.location.exception.LocationErrorCode;
import com.example.travel.domain.location.exception.LocationException;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class KakaoLocalClient {
    private static final String ACCOMMODATION_CATEGORY = "AD5";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final KakaoMapProperties properties;
    private final BodyFetcher bodyFetcher;

    @Autowired
    public KakaoLocalClient(KakaoMapProperties properties) {
        this(properties, createBodyFetcher());
    }

    KakaoLocalClient(KakaoMapProperties properties, BodyFetcher bodyFetcher) {
        this.properties = properties;
        this.bodyFetcher = bodyFetcher;
    }

    public LocationSearchResponse search(String query, BigDecimal latitude,
                                         BigDecimal longitude, int radius, int size) {
        return search(query, latitude, longitude, radius, size, ACCOMMODATION_CATEGORY);
    }

    public LocationSearchResponse searchRoutePoints(String query, BigDecimal latitude,
                                                     BigDecimal longitude, int radius, int size) {
        return search(query, latitude, longitude, radius, size, null);
    }

    private LocationSearchResponse search(String query, BigDecimal latitude,
                                          BigDecimal longitude, int radius, int size,
                                          String categoryGroupCode) {
        if (!properties.hasRestApiKey()) {
            throw new LocationException(LocationErrorCode.MISSING_API_KEY);
        }

        try {
            String body = bodyFetcher.fetch(searchUri(query, latitude, longitude, radius, size,
                            categoryGroupCode),
                    properties.restApiKey().trim());
            if (body == null || body.isBlank()) {
                throw new LocationException(LocationErrorCode.UPSTREAM_ERROR);
            }
            return parse(body);
        } catch (JsonProcessingException exception) {
            throw new LocationException(LocationErrorCode.UPSTREAM_ERROR, exception);
        } catch (RestClientException exception) {
            throw new LocationException(LocationErrorCode.UNAVAILABLE, exception);
        }
    }

    private URI searchUri(String query, BigDecimal latitude, BigDecimal longitude,
                          int radius, int size, String categoryGroupCode) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.baseUrl())
                .path("/v2/local/search/keyword.json")
                .queryParam("query", query.trim())
                .queryParam("size", Math.min(Math.max(size, 1), 15));
        if (categoryGroupCode != null && !categoryGroupCode.isBlank()) {
            builder.queryParam("category_group_code", categoryGroupCode);
        }
        if (latitude != null && longitude != null) {
            builder.queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("radius", Math.min(Math.max(radius, 1), 20_000))
                    .queryParam("sort", "distance");
        }
        return builder.build().encode().toUri();
    }

    private LocationSearchResponse parse(String body) throws JsonProcessingException {
        JsonNode documents = OBJECT_MAPPER.readTree(body).path("documents");
        if (!documents.isArray()) {
            throw new LocationException(LocationErrorCode.UPSTREAM_ERROR);
        }
        List<LocationSearchItem> items = new ArrayList<>();
        for (JsonNode document : documents) {
            items.add(new LocationSearchItem(
                    text(document, "id"),
                    text(document, "place_name"),
                    text(document, "category_name"),
                    text(document, "address_name"),
                    text(document, "road_address_name"),
                    decimal(document, "y"),
                    decimal(document, "x"),
                    integer(document, "distance"),
                    text(document, "place_url")));
        }
        return new LocationSearchResponse(List.copyOf(items));
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

    private Integer integer(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static BodyFetcher createBodyFetcher() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return (uri, apiKey) -> restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + apiKey)
                .retrieve()
                .body(String.class);
    }

    @FunctionalInterface
    interface BodyFetcher {
        String fetch(URI uri, String apiKey);
    }
}
