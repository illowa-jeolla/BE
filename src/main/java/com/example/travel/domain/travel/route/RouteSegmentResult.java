package com.example.travel.domain.travel.route;

import java.util.List;

public record RouteSegmentResult(int distanceMeters, int durationMinutes,
                                 List<Coordinate> path, boolean estimated) {
    public record Coordinate(double latitude, double longitude) {
    }
}
