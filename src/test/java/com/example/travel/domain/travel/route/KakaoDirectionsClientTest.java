package com.example.travel.domain.travel.route;

import com.example.travel.domain.location.config.KakaoMapProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoDirectionsClientTest {
    private final KakaoMapProperties properties =
            new KakaoMapProperties("rest-api-key", "https://dapi.kakao.com");

    @Test
    void requestsOneRouteAndMapsEachSectionToASegment() {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        AtomicReference<Map<String, Object>> requestedBody = new AtomicReference<>();
        KakaoDirectionsClient client = new KakaoDirectionsClient(properties, (url, body) -> {
            requestedUrl.set(url);
            requestedBody.set(body);
            return """
                    {"routes":[{"result_code":0,"sections":[
                      {"distance":1200,"duration":61,"roads":[{"vertexes":[127.1,34.1,127.2,34.2]}]},
                      {"distance":800,"duration":120,"roads":[{"vertexes":[127.2,34.2,127.3,34.3]}]}
                    ]}]}
                    """;
        });

        List<RouteSegmentResult> result = client.directions(point("출발", "34.0", "127.0"),
                List.of(point("경유", "34.2", "127.2")), point("도착", "34.3", "127.3"));

        assertThat(requestedUrl.get()).isEqualTo(KakaoDirectionsClient.DIRECTIONS_URL);
        assertThat(requestedBody.get()).containsEntry("priority", "RECOMMEND")
                .containsEntry("summary", false);
        assertThat((List<?>) requestedBody.get().get("waypoints")).hasSize(1);
        assertThat(result).extracting(RouteSegmentResult::distanceMeters)
                .containsExactly(1200, 800);
        assertThat(result).extracting(RouteSegmentResult::durationMinutes)
                .containsExactly(2, 2);
        assertThat(result.get(0).path()).containsExactly(
                new RouteSegmentResult.Coordinate(34.1, 127.1),
                new RouteSegmentResult.Coordinate(34.2, 127.2));
        assertThat(result).allMatch(segment -> !segment.estimated());
    }

    @Test
    void rejectsMoreThanThirtyWaypointsBeforeCallingKakao() {
        KakaoDirectionsClient client = new KakaoDirectionsClient(properties,
                (url, body) -> { throw new AssertionError("호출되면 안 됩니다."); });
        List<RoutePoint> waypoints = new ArrayList<>();
        for (int index = 0; index < 31; index++) {
            waypoints.add(point("경유" + index, "34.2", "127.2"));
        }

        assertThatThrownBy(() -> client.directions(point("출발", "34", "127"),
                waypoints, point("도착", "35", "128")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 30개");
    }

    @Test
    void preservesKakaoFailureMessage() {
        KakaoDirectionsClient client = new KakaoDirectionsClient(properties, (url, body) ->
                "{\"routes\":[{\"result_code\":107,\"result_msg\":\"경유지 주변 통제\"}]}");

        assertThatThrownBy(() -> client.directions(point("출발", "34", "127"),
                List.of(), point("도착", "35", "128")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("경유지 주변 통제");
    }

    private RoutePoint point(String name, String latitude, String longitude) {
        return new RoutePoint(name, new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
