package com.example.travel.domain.location.client;

import com.example.travel.domain.location.config.KakaoMapProperties;
import com.example.travel.domain.location.dto.LocationSearchResponse;
import com.example.travel.domain.location.exception.LocationErrorCode;
import com.example.travel.domain.location.exception.LocationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoLocalClientTest {
    private final KakaoMapProperties properties = new KakaoMapProperties(
            "rest-api-key", "https://dapi.kakao.com");

    @Test
    void searchReturnsAccommodationCoordinatesAndUsesDistanceSort() {
        List<URI> requestedUris = new ArrayList<>();
        List<String> apiKeys = new ArrayList<>();
        KakaoLocalClient client = new KakaoLocalClient(properties, (uri, apiKey) -> {
            requestedUris.add(uri);
            apiKeys.add(apiKey);
            return """
                    {
                      "documents": [
                        {
                          "id": "12345",
                          "place_name": "여수엑스포역",
                          "category_name": "교통,수송 > 기차역",
                          "address_name": "전남 여수시 덕충동",
                          "road_address_name": "전남 여수시 망양로 2",
                          "x": "127.748",
                          "y": "34.753",
                          "distance": "350",
                          "place_url": "https://place.map.kakao.com/12345"
                        }
                      ]
                    }
                    """;
        });

        LocationSearchResponse response = client.search("여수엑스포역",
                new BigDecimal("34.75"), new BigDecimal("127.74"), 50_000, 10);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).name()).isEqualTo("여수엑스포역");
        assertThat(response.items().get(0).latitude()).isEqualByComparingTo("34.753");
        assertThat(response.items().get(0).longitude()).isEqualByComparingTo("127.748");
        assertThat(response.items().get(0).distanceMeters()).isEqualTo(350);
        assertThat(requestedUris.get(0).toString())
                .contains("/v2/local/search/keyword.json?", "query=",
                        "category_group_code=AD5", "x=127.74",
                        "y=34.75", "radius=20000", "sort=distance", "size=10");
        assertThat(apiKeys).containsExactly("rest-api-key");
    }

    @Test
    void searchWithoutCenterDoesNotRequestDistanceSort() {
        List<URI> requestedUris = new ArrayList<>();
        KakaoLocalClient client = new KakaoLocalClient(properties, (uri, apiKey) -> {
            requestedUris.add(uri);
            return "{\"documents\": []}";
        });

        client.search("여수엑스포역", null, null, 20_000, 20);

        assertThat(requestedUris.get(0).toString())
                .contains("category_group_code=AD5", "size=15")
                .doesNotContain("sort=", "&x=", "&y=");
    }

    @Test
    void routePointSearchDoesNotRestrictResultsToAccommodationCategory() {
        List<URI> requestedUris = new ArrayList<>();
        KakaoLocalClient client = new KakaoLocalClient(properties, (uri, apiKey) -> {
            requestedUris.add(uri);
            return "{\"documents\": []}";
        });

        client.searchRoutePoints("전주역", new BigDecimal("35.82"),
                new BigDecimal("127.15"), 20_000, 10);

        assertThat(requestedUris.get(0).toString())
                .contains("/v2/local/search/keyword.json?", "query=", "x=127.15",
                        "y=35.82", "radius=20000", "sort=distance", "size=10")
                .doesNotContain("category_group_code");
    }

    @Test
    void searchRejectsMissingApiKey() {
        KakaoLocalClient client = new KakaoLocalClient(
                new KakaoMapProperties("", "https://dapi.kakao.com"),
                (uri, apiKey) -> "{}");

        assertThatThrownBy(() -> client.search("숙소", null, null, 20_000, 10))
                .isInstanceOf(LocationException.class)
                .extracting("code")
                .isEqualTo(LocationErrorCode.MISSING_API_KEY.code());
    }

    @Test
    void searchRejectsMalformedResponse() {
        KakaoLocalClient client = new KakaoLocalClient(properties,
                (uri, apiKey) -> "{\"documents\": {}}");

        assertThatThrownBy(() -> client.search("숙소", null, null, 20_000, 10))
                .isInstanceOf(LocationException.class)
                .extracting("code")
                .isEqualTo(LocationErrorCode.UPSTREAM_ERROR.code());
    }
}
