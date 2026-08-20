package com.example.travel.domain.travel.service;

import com.example.travel.domain.tour.dto.TourPlaceItem;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import com.example.travel.domain.tour.service.TourPlaceService;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.TravelTheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelCandidateServiceTest {
    @Mock private TourPlaceService tourPlaceService;
    @InjectMocks private TravelCandidateService service;

    @Test
    void scoresAndSortsCandidatesUsingDistanceThemeAndTransport() {
        BigDecimal latitude = new BigDecimal("34.3110");
        BigDecimal longitude = new BigDecimal("126.7551");
        when(tourPlaceService.findNearbyPlaces(latitude, longitude, 20_000, 1, 30))
                .thenReturn(new TourPlaceMapResponse(1, 30, 2, List.of(
                        place("1", "완도 해변공원", "A01", 1_000),
                        place("2", "완도 기념관", "A02", 500))));

        List<TravelCandidateItem> result = service.findCandidates(latitude, longitude,
                Set.of(TravelTheme.NATURE_HEALING), TransportType.WALK);

        assertThat(result).extracting(TravelCandidateItem::contentId)
                .containsExactly("1", "2");
        assertThat(result.get(0).baseScore()).isGreaterThan(result.get(1).baseScore());
    }

    @Test
    void removesCandidatesWithoutCoordinates() {
        BigDecimal latitude = new BigDecimal("34.3110");
        BigDecimal longitude = new BigDecimal("126.7551");
        TourPlaceItem invalid = new TourPlaceItem("1", "12", "A01", null, null,
                "좌표 없는 장소", "주소", null, null, null, 100);
        when(tourPlaceService.findNearbyPlaces(latitude, longitude, 20_000, 1, 30))
                .thenReturn(new TourPlaceMapResponse(1, 30, 1, List.of(invalid)));

        assertThat(service.findCandidates(latitude, longitude,
                Set.of(TravelTheme.PHOTO), TransportType.CAR)).isEmpty();
    }

    private TourPlaceItem place(String id, String title, String category1, int distance) {
        return new TourPlaceItem(id, "12", category1, null, null, title, "주소", null,
                new BigDecimal("126.75"), new BigDecimal("34.31"), distance);
    }
}
