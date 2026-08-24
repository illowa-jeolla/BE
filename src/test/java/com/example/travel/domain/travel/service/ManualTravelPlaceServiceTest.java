package com.example.travel.domain.travel.service;

import com.example.travel.domain.tour.dto.TourPlaceItem;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import com.example.travel.domain.tour.service.TourPlaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualTravelPlaceServiceTest {
    @Mock private TourPlaceService tourPlaceService;
    @InjectMocks private ManualTravelPlaceService service;

    @Test
    void findsTheRequestedTourApiPageAroundTheAccommodation() {
        var latitude = new java.math.BigDecimal("35.815");
        var longitude = new java.math.BigDecimal("127.153");
        TourPlaceItem place = new TourPlaceItem(
                "126273", "12", "A02", "A0201", "A02011000",
                "전주한옥마을", "전북특별자치도 전주시", "thumb",
                longitude, latitude, 1500);
        when(tourPlaceService.findNearbyPlaces(latitude, longitude, 20_000, 2, 30))
                .thenReturn(new TourPlaceMapResponse(2, 30, 75, List.of(place)));

        var response = service.findPlaces(latitude, longitude, 2);

        verify(tourPlaceService).findNearbyPlaces(latitude, longitude, 20_000, 2, 30);
        assertThat(response.pageNo()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasPrevious()).isTrue();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.contentId()).isEqualTo("126273");
            assertThat(item.latitude()).isEqualByComparingTo(latitude);
            assertThat(item.longitude()).isEqualByComparingTo(longitude);
        });
    }

    @Test
    void marksTheLastPageWithoutAFollowingPage() {
        var latitude = new java.math.BigDecimal("35.815");
        var longitude = new java.math.BigDecimal("127.153");
        when(tourPlaceService.findNearbyPlaces(latitude, longitude, 20_000, 3, 30))
                .thenReturn(new TourPlaceMapResponse(3, 30, 75, List.of()));

        var response = service.findPlaces(latitude, longitude, 3);

        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isFalse();
    }
}
