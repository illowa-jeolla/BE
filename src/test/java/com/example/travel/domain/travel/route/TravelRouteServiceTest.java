package com.example.travel.domain.travel.route;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import com.example.travel.domain.travel.enums.TransportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelRouteServiceTest {
    @Mock KakaoDirectionsClient client;
    @Mock TravelRecommendationRequest request;

    @Test
    void createsFirstMiddleAndLastDayBoundaries() {
        when(request.getTransportType()).thenReturn(TransportType.CAR);
        when(request.getStartPlaceName()).thenReturn("출발지");
        when(request.getStartLatitude()).thenReturn(bd("34.1"));
        when(request.getStartLongitude()).thenReturn(bd("127.1"));
        when(request.getEndPlaceName()).thenReturn("도착지");
        when(request.getEndLatitude()).thenReturn(bd("34.9"));
        when(request.getEndLongitude()).thenReturn(bd("127.9"));
        when(request.getLodgingName()).thenReturn("숙소");
        when(request.getLodgingLatitude()).thenReturn(bd("34.5"));
        when(request.getLodgingLongitude()).thenReturn(bd("127.5"));
        when(client.directions(any(), anyList(), any())).thenAnswer(invocation -> {
            List<RoutePoint> waypoints = invocation.getArgument(1);
            return java.util.Collections.nCopies(waypoints.size() + 1,
                    new RouteSegmentResult(1000, 5, List.of(), false));
        });

        List<TravelCandidateItem> candidates = List.of(candidate("1", "A"),
                candidate("2", "B"), candidate("3", "C"));
        AiTravelGuideResult guide = new AiTravelGuideResult("제목", "요약", List.of(
                day(1, "1"), day(2, "2"), day(3, "3")), "팁");

        List<PlannedRouteSegment> routes = new TravelRouteService(client)
                .plan(request, guide, candidates);

        assertThat(routes).extracting(route -> route.from().name() + "->" + route.to().name())
                .containsExactly("출발지->A", "A->숙소", "숙소->B", "B->숙소",
                        "숙소->C", "C->도착지");
        verify(client, times(3)).directions(any(), anyList(), any());
    }

    @Test
    void singleDayUsesStartAndEndLocations() {
        when(request.getTransportType()).thenReturn(TransportType.CAR);
        when(request.getStartPlaceName()).thenReturn("출발지");
        when(request.getStartLatitude()).thenReturn(bd("34.1"));
        when(request.getStartLongitude()).thenReturn(bd("127.1"));
        when(request.getEndPlaceName()).thenReturn("도착지");
        when(request.getEndLatitude()).thenReturn(bd("34.9"));
        when(request.getEndLongitude()).thenReturn(bd("127.9"));
        when(client.directions(any(), anyList(), any())).thenReturn(List.of(
                new RouteSegmentResult(1000, 5, List.of(), false),
                new RouteSegmentResult(1000, 5, List.of(), false)));

        List<PlannedRouteSegment> routes = new TravelRouteService(client).plan(request,
                new AiTravelGuideResult("제목", "요약", List.of(day(1, "1")), "팁"),
                List.of(candidate("1", "A")));

        assertThat(routes).extracting(route -> route.from().name() + "->" + route.to().name())
                .containsExactly("출발지->A", "A->도착지");
        verify(client).directions(any(), anyList(), any());
    }

    private AiTravelGuideResult.Day day(int number, String id) {
        return new AiTravelGuideResult.Day(number, "일정", List.of(
                new AiTravelGuideResult.Item(id, 1, "10:00", 60, "추천")));
    }

    private TravelCandidateItem candidate(String id, String title) {
        return new TravelCandidateItem(id, title, "주소", null, bd("34.4"), bd("127.4"), 10, 80);
    }

    private BigDecimal bd(String value) { return new BigDecimal(value); }
}
