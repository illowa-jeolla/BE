package com.example.travel.domain.tour.client;

import com.example.travel.domain.tour.config.TourInfoProperties;
import com.example.travel.domain.tour.dto.TourPlaceDetailResponse;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import com.example.travel.domain.tour.exception.TourErrorCode;
import com.example.travel.domain.tour.exception.TourException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourInfoClientTest {
    private final TourInfoProperties properties = new TourInfoProperties(
            "service-key", "http://apis.data.go.kr/B551011/KorService2", "ETC", "tour_gong");

    @Test
    void findPlacesReturnsContentIdsAndContentTypeIds() {
        List<URI> requestedUris = new ArrayList<>();
        TourInfoClient client = new TourInfoClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                    {
                      "response": {
                        "header": { "resultCode": "0000" },
                        "body": {
                          "pageNo": 1,
                          "numOfRows": 12,
                          "totalCount": 119,
                          "items": {
                            "item": [
                              {
                                "contentid": "126273",
                                "contenttypeid": "12",
                                "title": "전주한옥마을",
                                "addr1": "전북특별자치도 전주시 완산구",
                                "firstimage2": "https://example.com/thumb.jpg",
                                "mapx": "127.153",
                                "mapy": "35.815"
                              }
                            ]
                          }
                        }
                      }
                    }
                    """;
        });

        TourPlaceMapResponse response = client.findPlaces("전주", 1, 12);

        assertThat(response.pageNo()).isEqualTo(1);
        assertThat(response.numOfRows()).isEqualTo(12);
        assertThat(response.totalCount()).isEqualTo(119);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).contentId()).isEqualTo("126273");
        assertThat(response.items().get(0).contentTypeId()).isEqualTo("12");
        assertThat(response.items().get(0).mapX()).isEqualByComparingTo("127.153");
        assertThat(response.items().get(0).mapY()).isEqualByComparingTo("35.815");
        assertThat(requestedUris.get(0).toString())
                .contains("/locationBasedList2?", "MobileOS=ETC", "MobileApp=tour_gong")
                .doesNotContain("/locationBasedList2/locationBasedList2");
    }

    @Test
    void findPlaceDetailReturnsDetailFieldsAndMergedAddress() {
        List<URI> requestedUris = new ArrayList<>();
        TourInfoClient client = new TourInfoClient(properties, uri -> {
            requestedUris.add(uri);
            return """
                {
                  "response": {
                    "header": { "resultCode": "0000" },
                    "body": {
                      "items": {
                        "item": {
                          "contentid": "126273",
                          "contenttypeid": "12",
                          "title": "전주한옥마을",
                          "addr1": "전북특별자치도 전주시 완산구",
                          "addr2": "기린대로 99",
                          "tel": "063-000-0000",
                          "homepage": "https://example.com",
                          "overview": "관광지 설명",
                          "firstimage": "https://example.com/image.jpg",
                          "firstimage2": "https://example.com/thumb.jpg",
                          "mapx": "127.153",
                          "mapy": "35.815",
                          "zipcode": "55041"
                        }
                      }
                    }
                  }
                }
                """;
        });

        TourPlaceDetailResponse detail = client.findPlaceDetail("126273");

        assertThat(detail.contentId()).isEqualTo("126273");
        assertThat(detail.contentTypeId()).isEqualTo("12");
        assertThat(detail.address()).isEqualTo("전북특별자치도 전주시 완산구 기린대로 99");
        assertThat(detail.tel()).isEqualTo("063-000-0000");
        assertThat(detail.homepage()).isEqualTo("https://example.com");
        assertThat(detail.overview()).isEqualTo("관광지 설명");
        assertThat(detail.firstImage()).isEqualTo("https://example.com/image.jpg");
        assertThat(detail.firstImageThumbnail()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(detail.mapX()).isEqualByComparingTo("127.153");
        assertThat(detail.mapY()).isEqualByComparingTo("35.815");
        assertThat(detail.zipcode()).isEqualTo("55041");
        assertThat(requestedUris.get(0).toString())
                .contains("/detailCommon2?", "contentId=126273")
                .doesNotContain("defaultYN", "firstImageYN", "addrinfoYN", "mapinfoYN", "overviewYN");
    }

    @Test
    void findPlaceDetailHandlesMissingOptionalFields() {
        TourInfoClient client = new TourInfoClient(properties, uri -> """
                {
                  "response": {
                    "header": { "resultCode": "0000" },
                    "body": {
                      "items": {
                        "item": {
                          "contentid": "126273",
                          "contenttypeid": "12",
                          "title": "전주한옥마을",
                          "addr1": "전북특별자치도 전주시 완산구"
                        }
                      }
                    }
                  }
                }
                """);

        TourPlaceDetailResponse detail = client.findPlaceDetail("126273");

        assertThat(detail.address()).isEqualTo("전북특별자치도 전주시 완산구");
        assertThat(detail.tel()).isNull();
        assertThat(detail.homepage()).isNull();
        assertThat(detail.mapX()).isNull();
        assertThat(detail.mapY()).isNull();
    }

    @Test
    void findPlaceDetailThrowsWhenItemIsMissing() {
        TourInfoClient client = new TourInfoClient(properties, uri -> """
                {
                  "response": {
                    "header": { "resultCode": "0000" },
                    "body": {
                      "items": ""
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> client.findPlaceDetail("missing"))
                .isInstanceOf(TourException.class)
                .extracting("code")
                .isEqualTo(TourErrorCode.NOT_FOUND.code());
    }

    @Test
    void throwsUpstreamErrorWhenJsonParsingFails() {
        TourInfoClient client = new TourInfoClient(properties, uri -> "{");

        assertThatThrownBy(() -> client.findPlaceDetail("126273"))
                .isInstanceOf(TourException.class)
                .extracting("code")
                .isEqualTo(TourErrorCode.UPSTREAM_ERROR.code());
    }

    @Test
    void throwsMissingServiceKeyWhenServiceKeyIsBlank() {
        TourInfoClient client = new TourInfoClient(
                new TourInfoProperties("", "http://apis.data.go.kr/B551011/KorService2", "ETC", "tour_gong"),
                uri -> "{}");

        assertThatThrownBy(() -> client.findPlaces("전주", 1, 12))
                .isInstanceOf(TourException.class)
                .extracting("code")
                .isEqualTo(TourErrorCode.MISSING_SERVICE_KEY.code());
    }
}
