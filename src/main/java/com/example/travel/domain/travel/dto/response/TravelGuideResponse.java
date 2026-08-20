package com.example.travel.domain.travel.dto.response;

import com.example.travel.domain.travel.enums.GuideStatus;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record TravelGuideResponse(
        Long draftId,
        Long guideId,
        String title,
        String summary,
        String travelTip,
        boolean generatedByAi,
        boolean refreshAvailable,
        GuideStatus status,
        List<Day> days
) {
    public record Day(short dayNumber, List<Item> items, List<RouteSegment> routeSegments) {
    }

    public record Item(
            short order,
            String contentId,
            String title,
            String reason,
            LocalTime recommendedTime,
            Integer stayMinutes,
            Integer travelMinutes,
            BigDecimal latitude,
            BigDecimal longitude,
            String thumbnailUrl
    ) {
    }

    public record RouteSegment(short order, String fromName, String toName,
                               int distanceMeters, int durationMinutes, boolean estimated,
                               List<Coordinate> path) {
    }

    public record Coordinate(double latitude, double longitude) {
    }
}
