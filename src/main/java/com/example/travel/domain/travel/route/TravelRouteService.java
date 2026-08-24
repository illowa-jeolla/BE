package com.example.travel.domain.travel.route;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TravelRouteService {
    private final KakaoDirectionsClient directionsClient;

    public TravelRouteService(KakaoDirectionsClient directionsClient) {
        this.directionsClient = directionsClient;
    }

    public List<PlannedRouteSegment> plan(TravelRecommendationRequest request,
            AiTravelGuideResult guide, List<TravelCandidateItem> candidates) {
        Map<String, TravelCandidateItem> byId = candidates.stream().collect(
                Collectors.toMap(TravelCandidateItem::contentId, Function.identity(),
                        (first, ignored) -> first));
        int lastDay = guide.days().size();
        List<PlannedRouteSegment> segments = new ArrayList<>();
        for (AiTravelGuideResult.Day day : guide.days()) {
            RoutePoint origin = startPoint(request, day.dayNumber(), lastDay);
            List<RoutePoint> waypoints = new ArrayList<>();
            for (AiTravelGuideResult.Item item : day.items()) {
                TravelCandidateItem candidate = byId.get(item.contentId());
                if (candidate == null) {
                    throw new TravelRecommendationException(
                            TravelRecommendationErrorCode.INVALID_AI_PLACE_ID);
                }
                waypoints.add(new RoutePoint(candidate.title(), candidate.latitude(), candidate.longitude()));
            }
            RoutePoint destination = endPoint(request, day.dayNumber(), lastDay);
            List<RoutePoint> allPoints = new ArrayList<>();
            allPoints.add(origin);
            allPoints.addAll(waypoints);
            allPoints.add(destination);
            List<RouteSegmentResult> dayRoutes = routesForDay(
                    origin, waypoints, destination, request.getTransportType());
            for (int index = 0; index < dayRoutes.size(); index++) {
                segments.add(new PlannedRouteSegment(day.dayNumber(), index + 1,
                        allPoints.get(index), allPoints.get(index + 1), dayRoutes.get(index)));
            }
        }
        return List.copyOf(segments);
    }

    private RoutePoint startPoint(TravelRecommendationRequest request, int day, int lastDay) {
        if (day == 1) return new RoutePoint(request.getStartPlaceName(),
                request.getStartLatitude(), request.getStartLongitude());
        return lodging(request);
    }

    private RoutePoint endPoint(TravelRecommendationRequest request, int day, int lastDay) {
        if (day == lastDay) return new RoutePoint(request.getEndPlaceName(),
                request.getEndLatitude(), request.getEndLongitude());
        return lodging(request);
    }

    private RoutePoint lodging(TravelRecommendationRequest request) {
        return new RoutePoint(request.getLodgingName(), request.getLodgingLatitude(),
                request.getLodgingLongitude());
    }

    private List<RouteSegmentResult> routesForDay(RoutePoint origin, List<RoutePoint> waypoints,
                                                   RoutePoint destination,
                                                   TransportType transportType) {
        if (transportType == TransportType.CAR) {
            try { return directionsClient.directions(origin, waypoints, destination); }
            catch (RuntimeException ignored) { /* 카카오 호출 실패 시 구간별 추정값으로 대체 */ }
        }
        List<RoutePoint> points = new ArrayList<>();
        points.add(origin);
        points.addAll(waypoints);
        points.add(destination);
        List<RouteSegmentResult> estimates = new ArrayList<>();
        for (int index = 0; index + 1 < points.size(); index++) {
            estimates.add(estimate(points.get(index), points.get(index + 1), transportType));
        }
        return List.copyOf(estimates);
    }

    private RouteSegmentResult estimate(RoutePoint from, RoutePoint to, TransportType type) {
        int meters = (int) Math.round(haversine(from, to));
        double metersPerMinute = type == TransportType.WALK ? 75.0
                : type == TransportType.PUBLIC_TRANSIT ? 300.0 : 500.0;
        int minutes = Math.max(1, (int) Math.ceil(meters / metersPerMinute));
        return new RouteSegmentResult(meters, minutes, List.of(
                new RouteSegmentResult.Coordinate(from.latitude().doubleValue(), from.longitude().doubleValue()),
                new RouteSegmentResult.Coordinate(to.latitude().doubleValue(), to.longitude().doubleValue())), true);
    }

    private double haversine(RoutePoint a, RoutePoint b) {
        double lat1 = Math.toRadians(a.latitude().doubleValue());
        double lat2 = Math.toRadians(b.latitude().doubleValue());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude().doubleValue() - a.longitude().doubleValue());
        double value = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }
}
