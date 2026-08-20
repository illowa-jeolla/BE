package com.example.travel.domain.location.service;

import com.example.travel.domain.location.client.KakaoLocalClient;
import com.example.travel.domain.location.dto.LocationSearchItem;
import com.example.travel.domain.location.dto.LocationSearchResponse;
import com.example.travel.domain.location.exception.LocationErrorCode;
import com.example.travel.domain.location.exception.LocationException;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationSearchServiceTest {
    @Mock private KakaoLocalClient kakaoLocalClient;
    @Mock private RegionRepository regionRepository;
    @Mock private Region region;

    private LocationSearchService service;

    @BeforeEach
    void setUp() {
        service = new LocationSearchService(kakaoLocalClient, regionRepository);
    }

    @Test
    void searchUsesRegionCenterAndExcludesOtherRegions() {
        when(regionRepository.findActiveById(10L)).thenReturn(Optional.of(region));
        when(region.getName()).thenReturn("여수");
        when(region.getLatitude()).thenReturn(new BigDecimal("34.7604"));
        when(region.getLongitude()).thenReturn(new BigDecimal("127.6622"));
        when(kakaoLocalClient.search("베네치아", new BigDecimal("34.7604"),
                new BigDecimal("127.6622"), 20_000, 15))
                .thenReturn(new LocationSearchResponse(List.of(
                        item("여수 베네치아 호텔", "전남 여수시 오동도로 61"),
                        item("순천 베네치아 호텔", "전남 순천시 팔마로 13"))));

        LocationSearchResponse response = service.search(10L, "베네치아", 10);

        assertThat(response.items())
                .extracting(LocationSearchItem::name)
                .containsExactly("여수 베네치아 호텔");
        verify(kakaoLocalClient).search("베네치아", new BigDecimal("34.7604"),
                new BigDecimal("127.6622"), 20_000, 15);
    }

    @Test
    void searchAcceptsRegionNameWithAdministrativeSuffix() {
        when(regionRepository.findActiveById(10L)).thenReturn(Optional.of(region));
        when(region.getName()).thenReturn("여수시");
        when(region.getLatitude()).thenReturn(new BigDecimal("34.7604"));
        when(region.getLongitude()).thenReturn(new BigDecimal("127.6622"));
        when(kakaoLocalClient.search("호텔", new BigDecimal("34.7604"),
                new BigDecimal("127.6622"), 20_000, 15))
                .thenReturn(new LocationSearchResponse(List.of(
                        item("호텔", "전라남도 여수시 수정동 1"))));

        assertThat(service.search(10L, "호텔", 10).items()).hasSize(1);
    }

    @Test
    void routePointSearchUsesUnrestrictedKakaoKeywordSearch() {
        when(regionRepository.findActiveById(10L)).thenReturn(Optional.of(region));
        when(region.getName()).thenReturn("전주");
        when(region.getLatitude()).thenReturn(new BigDecimal("35.8242"));
        when(region.getLongitude()).thenReturn(new BigDecimal("127.1480"));
        when(kakaoLocalClient.searchRoutePoints("전주역", new BigDecimal("35.8242"),
                new BigDecimal("127.1480"), 20_000, 15))
                .thenReturn(new LocationSearchResponse(List.of(
                        item("전주역", "전북특별자치도 전주시 덕진구 동부대로 680"),
                        item("익산역", "전북특별자치도 익산시 익산대로 153"))));

        LocationSearchResponse response = service.searchRoutePoints(10L, "전주역", 10);

        assertThat(response.items()).extracting(LocationSearchItem::name)
                .containsExactly("전주역");
        verify(kakaoLocalClient).searchRoutePoints("전주역", new BigDecimal("35.8242"),
                new BigDecimal("127.1480"), 20_000, 15);
    }

    @Test
    void searchRejectsUnknownRegion() {
        when(regionRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.search(999L, "호텔", 10))
                .isInstanceOf(LocationException.class)
                .extracting("code")
                .isEqualTo(LocationErrorCode.REGION_NOT_FOUND.code());
    }

    @Test
    void searchRejectsRegionWithoutCoordinates() {
        when(regionRepository.findActiveById(10L)).thenReturn(Optional.of(region));
        when(region.getLatitude()).thenReturn(null);

        assertThatThrownBy(() -> service.search(10L, "호텔", 10))
                .isInstanceOf(LocationException.class)
                .extracting("code")
                .isEqualTo(LocationErrorCode.REGION_COORDINATES_MISSING.code());
    }

    private LocationSearchItem item(String name, String roadAddress) {
        return new LocationSearchItem("1", name, "숙박 > 호텔", null, roadAddress,
                new BigDecimal("34.75"), new BigDecimal("127.74"), 100, null);
    }
}
