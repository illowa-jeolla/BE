package com.example.travel.domain.travel.route;

import com.example.travel.domain.location.config.KakaoMapProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KakaoDirectionsClient {
    static final String DIRECTIONS_URL =
            "https://apis-navi.kakaomobility.com/v1/waypoints/directions";
    static final int MAX_WAYPOINTS = 30;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final KakaoMapProperties properties;
    private final BodyFetcher bodyFetcher;

    @Autowired
    public KakaoDirectionsClient(KakaoMapProperties properties) {
        this(properties, restClientFetcher(properties.restApiKey()));
    }

    KakaoDirectionsClient(KakaoMapProperties properties, BodyFetcher bodyFetcher) {
        this.properties = properties;
        this.bodyFetcher = bodyFetcher;
    }

    public List<RouteSegmentResult> directions(RoutePoint origin,
                                                List<RoutePoint> waypoints,
                                                RoutePoint destination) {
        validateRequest(origin, waypoints, destination);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("origin", point(origin));
        request.put("destination", point(destination));
        request.put("waypoints", waypoints.stream().map(this::point).toList());
        request.put("priority", "RECOMMEND");
        request.put("alternatives", false);
        request.put("road_details", false);
        request.put("summary", false);

        String body = bodyFetcher.post(DIRECTIONS_URL, request);
        try {
            JsonNode route = OBJECT_MAPPER.readTree(body).path("routes").path(0);
            if (route.isMissingNode() || route.path("result_code").asInt(-1) != 0) {
                String message = route.path("result_msg").asText("경로를 찾을 수 없습니다.");
                throw new IllegalStateException("카카오 다중 경유지 길찾기 실패: " + message);
            }

            JsonNode sections = route.path("sections");
            if (!sections.isArray() || sections.size() != waypoints.size() + 1) {
                throw new IllegalStateException("카카오 경로 구간 수가 요청과 일치하지 않습니다.");
            }

            List<RouteSegmentResult> results = new ArrayList<>();
            for (JsonNode section : sections) {
                results.add(new RouteSegmentResult(section.path("distance").asInt(),
                        (int) Math.ceil(section.path("duration").asInt() / 60.0),
                        path(section), false));
            }
            return List.copyOf(results);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("카카오 다중 경유지 응답 처리에 실패했습니다.", exception);
        }
    }

    private void validateRequest(RoutePoint origin, List<RoutePoint> waypoints,
                                 RoutePoint destination) {
        if (properties.restApiKey() == null || properties.restApiKey().isBlank()) {
            throw new IllegalStateException("카카오 지도 REST API 키가 설정되어 있지 않습니다.");
        }
        if (origin == null || destination == null || waypoints == null) {
            throw new IllegalArgumentException("출발지, 경유지, 목적지는 필수입니다.");
        }
        if (waypoints.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException("카카오 다중 경유지는 최대 30개까지 허용됩니다.");
        }
    }

    private Map<String, Object> point(RoutePoint point) {
        return Map.of("name", point.name(), "x", point.longitude(), "y", point.latitude());
    }

    private List<RouteSegmentResult.Coordinate> path(JsonNode section) {
        List<RouteSegmentResult.Coordinate> path = new ArrayList<>();
        for (JsonNode road : section.path("roads")) {
            JsonNode vertexes = road.path("vertexes");
            for (int index = 0; index + 1 < vertexes.size(); index += 2) {
                path.add(new RouteSegmentResult.Coordinate(
                        vertexes.get(index + 1).asDouble(), vertexes.get(index).asDouble()));
            }
        }
        return List.copyOf(path);
    }

    private static BodyFetcher restClientFetcher(String apiKey) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        RestClient client = RestClient.builder().requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .build();
        return (url, request) -> client.post().uri(url).body(request)
                .retrieve().body(String.class);
    }

    @FunctionalInterface
    interface BodyFetcher {
        String post(String url, Map<String, Object> request);
    }
}
